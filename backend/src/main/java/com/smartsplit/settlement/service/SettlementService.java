package com.smartsplit.settlement.service;

import com.smartsplit.balance.dto.GroupBalanceResponse;
import com.smartsplit.balance.dto.MemberBalanceResponse;
import com.smartsplit.balance.service.GroupBalanceService;
import com.smartsplit.common.exception.BusinessException;
import com.smartsplit.group.entity.*;
import com.smartsplit.group.repository.ExpenseGroupRepository;
import com.smartsplit.group.repository.GroupMemberRepository;
import com.smartsplit.settlement.dto.CreateSettlementRequest;
import com.smartsplit.settlement.dto.SettlementResponse;
import com.smartsplit.settlement.entity.Settlement;
import com.smartsplit.settlement.entity.SettlementStatus;
import com.smartsplit.settlement.repository.SettlementRepository;
import com.smartsplit.user.entity.User;
import com.smartsplit.user.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SettlementService {
    private final SettlementRepository settlementRepository;
    private final ExpenseGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final CurrentUserService currentUserService;
    private final GroupBalanceService groupBalanceService;

    public SettlementService(
            SettlementRepository settlementRepository,
            ExpenseGroupRepository groupRepository,
            GroupMemberRepository memberRepository,
            CurrentUserService currentUserService,
            GroupBalanceService groupBalanceService
    ) {
        this.settlementRepository = settlementRepository;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.currentUserService = currentUserService;
        this.groupBalanceService = groupBalanceService;
    }

    @Transactional(readOnly = true)
    public List<SettlementResponse> list(Long groupId) {
        User currentUser = currentUserService.getRequiredUser();
        getActiveGroup(groupId);
        requireActiveMembership(groupId, currentUser.getId());
        return settlementRepository.findAllByGroup_IdOrderBySettledAtDescCreatedAtDesc(groupId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SettlementResponse create(Long groupId, CreateSettlementRequest request) {
        User currentUser = currentUserService.getRequiredUser();
        ExpenseGroup group = getActiveGroup(groupId);
        GroupMember currentMembership = requireActiveMembership(groupId, currentUser.getId());
        GroupMember payerMembership = requireMembership(groupId, request.payerId());
        GroupMember receiverMembership = requireMembership(groupId, request.receiverId());

        if (request.payerId().equals(request.receiverId())) {
            throw new BusinessException("SAME_SETTLEMENT_USER", "Người trả và người nhận phải khác nhau");
        }
        boolean isManager = currentMembership.getRole() == GroupRole.OWNER
                || currentMembership.getRole() == GroupRole.ADMIN;
        boolean isRelatedUser = currentUser.getId().equals(request.payerId())
                || currentUser.getId().equals(request.receiverId());
        if (!isManager && !isRelatedUser) {
            throw new BusinessException(
                    "SETTLEMENT_ACCESS_DENIED",
                    "Bạn chỉ có thể ghi nhận giao dịch có liên quan đến mình",
                    HttpStatus.FORBIDDEN
            );
        }

        LocalDateTime settledAt = request.settledAt() == null
                ? LocalDateTime.now()
                : request.settledAt();
        if (settledAt.isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new BusinessException("INVALID_SETTLEMENT_TIME", "Thời gian thanh toán không được ở tương lai");
        }

        GroupBalanceResponse balances = groupBalanceService.calculateForAuthorizedService(groupId);
        MemberBalanceResponse payerBalance = findMemberBalance(balances, request.payerId());
        MemberBalanceResponse receiverBalance = findMemberBalance(balances, request.receiverId());
        if (payerBalance.balance() >= 0) {
            throw new BusinessException("PAYER_HAS_NO_DEBT", "Người trả hiện không có số dư âm");
        }
        if (receiverBalance.balance() <= 0) {
            throw new BusinessException("RECEIVER_HAS_NO_CREDIT", "Người nhận hiện không có số dư dương");
        }
        long payerDebt;
        try {
            payerDebt = Math.negateExact(payerBalance.balance());
        } catch (ArithmeticException exception) {
            throw new BusinessException("AMOUNT_OVERFLOW", "Số tiền vượt quá giới hạn hệ thống");
        }
        long maximumAmount = Math.min(payerDebt, receiverBalance.balance());
        if (request.amount() > maximumAmount) {
            throw new BusinessException(
                    "SETTLEMENT_EXCEEDS_BALANCE",
                    "Số tiền tối đa có thể ghi nhận là " + maximumAmount + " đồng"
            );
        }

        Settlement settlement = new Settlement();
        settlement.setGroup(group);
        settlement.setPayer(payerMembership.getUser());
        settlement.setReceiver(receiverMembership.getUser());
        settlement.setAmount(request.amount());
        settlement.setNote(normalizeNullable(request.note()));
        settlement.setStatus(SettlementStatus.CONFIRMED);
        settlement.setSettledAt(settledAt);
        settlement.setCreatedBy(currentUser);
        return toResponse(settlementRepository.save(settlement));
    }

    @Transactional
    public void cancel(Long settlementId) {
        User currentUser = currentUserService.getRequiredUser();
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new BusinessException(
                        "SETTLEMENT_NOT_FOUND",
                        "Không tìm thấy giao dịch thanh toán",
                        HttpStatus.NOT_FOUND
                ));
        GroupMember currentMembership = requireActiveMembership(
                settlement.getGroup().getId(),
                currentUser.getId()
        );
        boolean isManager = currentMembership.getRole() == GroupRole.OWNER
                || currentMembership.getRole() == GroupRole.ADMIN;
        boolean isCreator = settlement.getCreatedBy().getId().equals(currentUser.getId());
        if (!isManager && !isCreator) {
            throw new BusinessException(
                    "SETTLEMENT_CANCEL_DENIED",
                    "Chỉ người tạo hoặc quản lý nhóm có thể hủy giao dịch",
                    HttpStatus.FORBIDDEN
            );
        }
        if (settlement.getStatus() == SettlementStatus.CANCELLED) {
            throw new BusinessException(
                    "SETTLEMENT_ALREADY_CANCELLED",
                    "Giao dịch này đã được hủy",
                    HttpStatus.CONFLICT
            );
        }
        settlement.setStatus(SettlementStatus.CANCELLED);
        settlementRepository.save(settlement);
    }

    private MemberBalanceResponse findMemberBalance(GroupBalanceResponse response, Long userId) {
        return response.members().stream()
                .filter(member -> member.userId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "MEMBER_NOT_FOUND",
                        "Không tìm thấy thành viên trong bảng công nợ",
                        HttpStatus.NOT_FOUND
                ));
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
                "MEMBER_NOT_ACTIVE",
                "Thành viên không còn hoạt động trong nhóm",
                HttpStatus.FORBIDDEN
        ));
    }

    private GroupMember requireMembership(Long groupId, Long userId) {
        return memberRepository.findByGroup_IdAndUser_Id(groupId, userId)
                .orElseThrow(() -> new BusinessException(
                        "MEMBER_NOT_FOUND",
                        "Người dùng không thuộc nhóm này",
                        HttpStatus.NOT_FOUND
                ));
    }

    private String normalizeNullable(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private SettlementResponse toResponse(Settlement settlement) {
        return new SettlementResponse(
                settlement.getId(),
                settlement.getGroup().getId(),
                settlement.getPayer().getId(),
                settlement.getPayer().getFullName(),
                settlement.getReceiver().getId(),
                settlement.getReceiver().getFullName(),
                settlement.getAmount(),
                settlement.getNote(),
                settlement.getStatus().name(),
                settlement.getSettledAt(),
                settlement.getCreatedBy().getId(),
                settlement.getCreatedBy().getFullName(),
                settlement.getCreatedAt()
        );
    }
}
