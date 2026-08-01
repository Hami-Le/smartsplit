package com.smartsplit.balance.service;

import com.smartsplit.balance.dto.BalanceEntry;
import com.smartsplit.balance.dto.GroupBalanceResponse;
import com.smartsplit.balance.dto.MemberBalanceResponse;
import com.smartsplit.balance.dto.TransferSuggestion;
import com.smartsplit.common.exception.BusinessException;
import com.smartsplit.expense.entity.Expense;
import com.smartsplit.expense.entity.ExpensePayer;
import com.smartsplit.expense.entity.ExpenseShare;
import com.smartsplit.expense.entity.ExpenseStatus;
import com.smartsplit.expense.repository.ExpenseRepository;
import com.smartsplit.group.entity.*;
import com.smartsplit.group.repository.ExpenseGroupRepository;
import com.smartsplit.group.repository.GroupMemberRepository;
import com.smartsplit.settlement.entity.Settlement;
import com.smartsplit.settlement.entity.SettlementStatus;
import com.smartsplit.settlement.repository.SettlementRepository;
import com.smartsplit.user.entity.User;
import com.smartsplit.user.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GroupBalanceService {
    private final ExpenseGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final CurrentUserService currentUserService;
    private final DebtSimplificationService debtSimplificationService;

    public GroupBalanceService(
            ExpenseGroupRepository groupRepository,
            GroupMemberRepository memberRepository,
            ExpenseRepository expenseRepository,
            SettlementRepository settlementRepository,
            CurrentUserService currentUserService,
            DebtSimplificationService debtSimplificationService
    ) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.expenseRepository = expenseRepository;
        this.settlementRepository = settlementRepository;
        this.currentUserService = currentUserService;
        this.debtSimplificationService = debtSimplificationService;
    }

    @Transactional(readOnly = true)
    public GroupBalanceResponse getBalances(Long groupId) {
        User currentUser = currentUserService.getRequiredUser();
        ExpenseGroup group = getActiveGroup(groupId);
        GroupMember membership = requireActiveMembership(groupId, currentUser.getId());
        return calculate(group, membership.getRole().name());
    }

    @Transactional(readOnly = true)
    public GroupBalanceResponse calculateForAuthorizedService(Long groupId) {
        ExpenseGroup group = getActiveGroup(groupId);
        return calculate(group, null);
    }

    private GroupBalanceResponse calculate(ExpenseGroup group, String currentUserRole) {
        Map<Long, Accumulator> accumulators = new LinkedHashMap<>();
        for (GroupMember member : memberRepository.findAllByGroup_IdOrderByJoinedAtAsc(group.getId())) {
            accumulators.put(member.getUser().getId(), new Accumulator(member.getUser(), member.getStatus()));
        }

        long totalExpense = 0L;
        List<Expense> expenses = expenseRepository
                .findAllByGroup_IdAndStatusOrderByExpenseDateDescCreatedAtDesc(
                        group.getId(),
                        ExpenseStatus.ACTIVE
                );
        for (Expense expense : expenses) {
            totalExpense = safeAdd(totalExpense, expense.getTotalAmount());
            for (ExpensePayer payer : expense.getPayers()) {
                accumulatorFor(accumulators, payer.getUser()).paid =
                        safeAdd(accumulatorFor(accumulators, payer.getUser()).paid, payer.getPaidAmount());
            }
            for (ExpenseShare share : expense.getShares()) {
                accumulatorFor(accumulators, share.getUser()).share =
                        safeAdd(accumulatorFor(accumulators, share.getUser()).share, share.getShareAmount());
            }
        }

        long totalSettled = 0L;
        List<Settlement> settlements = settlementRepository
                .findAllByGroup_IdAndStatusOrderBySettledAtDescCreatedAtDesc(
                        group.getId(),
                        SettlementStatus.CONFIRMED
                );
        for (Settlement settlement : settlements) {
            totalSettled = safeAdd(totalSettled, settlement.getAmount());
            Accumulator payer = accumulatorFor(accumulators, settlement.getPayer());
            Accumulator receiver = accumulatorFor(accumulators, settlement.getReceiver());
            payer.sent = safeAdd(payer.sent, settlement.getAmount());
            receiver.received = safeAdd(receiver.received, settlement.getAmount());
        }

        List<MemberBalanceResponse> members = accumulators.values().stream()
                .map(Accumulator::toResponse)
                .toList();

        long ledgerTotal = members.stream().mapToLong(MemberBalanceResponse::balance).sum();
        if (ledgerTotal != 0L) {
            throw new BusinessException(
                    "UNBALANCED_LEDGER",
                    "Dữ liệu công nợ không cân bằng, chênh lệch " + ledgerTotal + " đồng"
            );
        }

        List<TransferSuggestion> suggestions = debtSimplificationService.simplify(
                members.stream()
                        .map(member -> new BalanceEntry(
                                member.userId(),
                                member.fullName(),
                                member.balance()
                        ))
                        .toList()
        );

        return new GroupBalanceResponse(
                group.getId(),
                group.getName(),
                group.getDefaultCurrency(),
                currentUserRole,
                totalExpense,
                totalSettled,
                members,
                suggestions
        );
    }

    private Accumulator accumulatorFor(Map<Long, Accumulator> accumulators, User user) {
        return accumulators.computeIfAbsent(
                user.getId(),
                ignored -> new Accumulator(user, GroupMemberStatus.REMOVED)
        );
    }

    private ExpenseGroup getActiveGroup(Long groupId) {
        return groupRepository.findByIdAndStatus(groupId, GroupStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        "GROUP_NOT_FOUND",
                        "Không tìm thấy nhóm",
                        HttpStatus.NOT_FOUND
                ));
    }

    private GroupMember requireActiveMembership(Long groupId, Long userId) {
        return memberRepository.findByGroup_IdAndUser_IdAndStatus(
                groupId,
                userId,
                GroupMemberStatus.ACTIVE
        ).orElseThrow(() -> new BusinessException(
                "GROUP_ACCESS_DENIED",
                "Bạn không có quyền truy cập nhóm này",
                HttpStatus.FORBIDDEN
        ));
    }

    private long safeAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            throw new BusinessException("AMOUNT_OVERFLOW", "Số tiền vượt quá giới hạn hệ thống");
        }
    }

    private static final class Accumulator {
        private final User user;
        private final GroupMemberStatus membershipStatus;
        private long paid;
        private long share;
        private long sent;
        private long received;

        private Accumulator(User user, GroupMemberStatus membershipStatus) {
            this.user = user;
            this.membershipStatus = membershipStatus;
        }

        private MemberBalanceResponse toResponse() {
            long balance;
            try {
                balance = Math.addExact(
                        Math.subtractExact(paid, share),
                        Math.subtractExact(sent, received)
                );
            } catch (ArithmeticException exception) {
                throw new BusinessException("AMOUNT_OVERFLOW", "Số tiền vượt quá giới hạn hệ thống");
            }
            return new MemberBalanceResponse(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getAvatarUrl(),
                    membershipStatus.name(),
                    paid,
                    share,
                    sent,
                    received,
                    balance
            );
        }
    }
}
