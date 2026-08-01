package com.smartsplit.report.service;

import com.smartsplit.balance.dto.GroupBalanceResponse;
import com.smartsplit.balance.service.GroupBalanceService;
import com.smartsplit.common.exception.BusinessException;
import com.smartsplit.expense.entity.Expense;
import com.smartsplit.expense.entity.ExpensePayer;
import com.smartsplit.expense.entity.ExpenseShare;
import com.smartsplit.expense.entity.ExpenseStatus;
import com.smartsplit.expense.repository.ExpenseRepository;
import com.smartsplit.group.entity.*;
import com.smartsplit.group.repository.ExpenseGroupRepository;
import com.smartsplit.group.repository.GroupMemberRepository;
import com.smartsplit.report.dto.*;
import com.smartsplit.settlement.entity.Settlement;
import com.smartsplit.settlement.entity.SettlementStatus;
import com.smartsplit.settlement.repository.SettlementRepository;
import com.smartsplit.user.entity.User;
import com.smartsplit.user.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {
    private static final int DEFAULT_MONTH_COUNT = 6;
    private static final int MAX_MONTH_RANGE = 36;
    private static final DateTimeFormatter MONTH_LABEL_FORMAT = DateTimeFormatter.ofPattern("MM/yyyy");

    private final ExpenseGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final CurrentUserService currentUserService;
    private final GroupBalanceService groupBalanceService;

    public ReportService(
            ExpenseGroupRepository groupRepository,
            GroupMemberRepository memberRepository,
            ExpenseRepository expenseRepository,
            SettlementRepository settlementRepository,
            CurrentUserService currentUserService,
            GroupBalanceService groupBalanceService
    ) {
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.expenseRepository = expenseRepository;
        this.settlementRepository = settlementRepository;
        this.currentUserService = currentUserService;
        this.groupBalanceService = groupBalanceService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(Long groupId, LocalDate from, LocalDate to) {
        return loadSnapshot(groupId, from, to).dashboard();
    }

    @Transactional(readOnly = true)
    public ReportSnapshot getSnapshot(Long groupId, LocalDate from, LocalDate to) {
        return loadSnapshot(groupId, from, to);
    }

    private ReportSnapshot loadSnapshot(Long groupId, LocalDate requestedFrom, LocalDate requestedTo) {
        User currentUser = currentUserService.getRequiredUser();
        ExpenseGroup group = groupRepository.findByIdAndStatus(groupId, GroupStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        "GROUP_NOT_FOUND",
                        "Không tìm thấy nhóm",
                        HttpStatus.NOT_FOUND
                ));
        GroupMember currentMembership = memberRepository
                .findByGroup_IdAndUser_IdAndStatus(groupId, currentUser.getId(), GroupMemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(
                        "FORBIDDEN",
                        "Bạn không có quyền xem báo cáo của nhóm này",
                        HttpStatus.FORBIDDEN
                ));

        DateRange range = resolveRange(requestedFrom, requestedTo);
        List<Expense> expenses = expenseRepository
                .findAllByGroup_IdAndStatusOrderByExpenseDateDescCreatedAtDesc(groupId, ExpenseStatus.ACTIVE)
                .stream()
                .filter(expense -> !expense.getExpenseDate().isBefore(range.from()))
                .filter(expense -> !expense.getExpenseDate().isAfter(range.to()))
                .toList();

        List<Settlement> allSettlements = settlementRepository
                .findAllByGroup_IdOrderBySettledAtDescCreatedAtDesc(groupId);
        List<Settlement> confirmedSettlements = allSettlements.stream()
                .filter(settlement -> settlement.getStatus() == SettlementStatus.CONFIRMED)
                .filter(settlement -> settlement.getSettledAt() != null)
                .filter(settlement -> !settlement.getSettledAt().toLocalDate().isBefore(range.from()))
                .filter(settlement -> !settlement.getSettledAt().toLocalDate().isAfter(range.to()))
                .toList();

        GroupBalanceResponse balances = groupBalanceService.calculateForAuthorizedService(groupId);
        DashboardResponse dashboard = buildDashboard(
                group,
                currentMembership,
                range,
                expenses,
                confirmedSettlements,
                balances
        );

        List<ReportExpenseRow> expenseRows = expenses.stream()
                .map(this::toExpenseRow)
                .toList();
        List<ReportSettlementRow> settlementRows = allSettlements.stream()
                .filter(settlement -> settlement.getSettledAt() != null)
                .filter(settlement -> !settlement.getSettledAt().toLocalDate().isBefore(range.from()))
                .filter(settlement -> !settlement.getSettledAt().toLocalDate().isAfter(range.to()))
                .map(this::toSettlementRow)
                .toList();

        return new ReportSnapshot(dashboard, expenseRows, balances.members(), settlementRows);
    }

    private DashboardResponse buildDashboard(
            ExpenseGroup group,
            GroupMember currentMembership,
            DateRange range,
            List<Expense> expenses,
            List<Settlement> confirmedSettlements,
            GroupBalanceResponse balances
    ) {
        long totalExpense = expenses.stream()
                .mapToLong(Expense::getTotalAmount)
                .reduce(0L, this::safeAdd);
        int expenseCount = expenses.size();
        long averageExpense = expenseCount == 0 ? 0L : Math.round((double) totalExpense / expenseCount);
        Expense largest = expenses.stream()
                .max(Comparator.comparingLong(Expense::getTotalAmount))
                .orElse(null);
        long highestExpense = largest == null ? 0L : largest.getTotalAmount();
        long totalSettled = confirmedSettlements.stream()
                .mapToLong(Settlement::getAmount)
                .reduce(0L, this::safeAdd);
        long outstandingAmount = balances.members().stream()
                .filter(member -> member.balance() > 0)
                .mapToLong(member -> member.balance())
                .reduce(0L, this::safeAdd);

        return new DashboardResponse(
                group.getId(),
                group.getName(),
                group.getDefaultCurrency(),
                currentMembership.getRole().name(),
                range.from(),
                range.to(),
                totalExpense,
                expenseCount,
                averageExpense,
                highestExpense,
                totalSettled,
                outstandingAmount,
                largest == null ? null : toDashboardExpense(largest),
                buildCategoryBreakdown(expenses, totalExpense),
                buildMemberSpending(group.getId(), expenses, totalExpense),
                buildMonthlyTrend(range, expenses),
                expenses.stream().limit(6).map(this::toDashboardExpense).toList()
        );
    }

    private List<CategorySpendingResponse> buildCategoryBreakdown(
            List<Expense> expenses,
            long totalExpense
    ) {
        Map<String, CategoryAccumulator> values = new LinkedHashMap<>();
        for (Expense expense : expenses) {
            Long categoryId = expense.getCategory() == null ? null : expense.getCategory().getId();
            String key = categoryId == null ? "uncategorized" : String.valueOf(categoryId);
            CategoryAccumulator accumulator = values.computeIfAbsent(
                    key,
                    ignored -> new CategoryAccumulator(
                            categoryId,
                            expense.getCategory() == null ? "Chưa phân loại" : expense.getCategory().getName(),
                            expense.getCategory() == null ? "📦" : expense.getCategory().getIcon()
                    )
            );
            accumulator.amount = safeAdd(accumulator.amount, expense.getTotalAmount());
            accumulator.expenseCount++;
        }

        return values.values().stream()
                .sorted(Comparator.comparingLong((CategoryAccumulator value) -> value.amount).reversed())
                .map(value -> new CategorySpendingResponse(
                        value.categoryId,
                        value.categoryName,
                        value.icon,
                        value.amount,
                        value.expenseCount,
                        percentage(value.amount, totalExpense)
                ))
                .toList();
    }

    private List<MemberSpendingResponse> buildMemberSpending(
            Long groupId,
            List<Expense> expenses,
            long totalExpense
    ) {
        Map<Long, MemberAccumulator> values = new LinkedHashMap<>();
        for (GroupMember member : memberRepository.findAllByGroup_IdOrderByJoinedAtAsc(groupId)) {
            values.put(member.getUser().getId(), new MemberAccumulator(member));
        }
        for (Expense expense : expenses) {
            for (ExpensePayer payer : expense.getPayers()) {
                MemberAccumulator accumulator = values.computeIfAbsent(
                        payer.getUser().getId(),
                        ignored -> new MemberAccumulator(payer.getUser())
                );
                accumulator.paidAmount = safeAdd(accumulator.paidAmount, payer.getPaidAmount());
            }
            for (ExpenseShare share : expense.getShares()) {
                MemberAccumulator accumulator = values.computeIfAbsent(
                        share.getUser().getId(),
                        ignored -> new MemberAccumulator(share.getUser())
                );
                accumulator.shareAmount = safeAdd(accumulator.shareAmount, share.getShareAmount());
            }
        }

        return values.values().stream()
                .sorted(Comparator
                        .comparingLong((MemberAccumulator value) -> value.shareAmount)
                        .reversed()
                        .thenComparing(value -> value.fullName))
                .map(value -> new MemberSpendingResponse(
                        value.userId,
                        value.fullName,
                        value.email,
                        value.avatarUrl,
                        value.membershipStatus,
                        value.paidAmount,
                        value.shareAmount,
                        percentage(value.shareAmount, totalExpense)
                ))
                .toList();
    }

    private List<MonthlySpendingResponse> buildMonthlyTrend(
            DateRange range,
            List<Expense> expenses
    ) {
        Map<YearMonth, MonthlyAccumulator> values = new LinkedHashMap<>();
        YearMonth cursor = YearMonth.from(range.from());
        YearMonth end = YearMonth.from(range.to());
        while (!cursor.isAfter(end)) {
            values.put(cursor, new MonthlyAccumulator());
            cursor = cursor.plusMonths(1);
        }
        for (Expense expense : expenses) {
            YearMonth month = YearMonth.from(expense.getExpenseDate());
            MonthlyAccumulator accumulator = values.get(month);
            if (accumulator != null) {
                accumulator.amount = safeAdd(accumulator.amount, expense.getTotalAmount());
                accumulator.expenseCount++;
            }
        }
        return values.entrySet().stream()
                .map(entry -> new MonthlySpendingResponse(
                        entry.getKey().toString(),
                        MONTH_LABEL_FORMAT.format(entry.getKey()),
                        entry.getValue().amount,
                        entry.getValue().expenseCount
                ))
                .toList();
    }

    private DashboardExpenseResponse toDashboardExpense(Expense expense) {
        return new DashboardExpenseResponse(
                expense.getId(),
                expense.getTitle(),
                expense.getExpenseDate(),
                expense.getTotalAmount(),
                expense.getCategory() == null ? "Chưa phân loại" : expense.getCategory().getName(),
                expense.getCategory() == null ? "📦" : expense.getCategory().getIcon(),
                expense.getCreatedBy().getFullName()
        );
    }

    private ReportExpenseRow toExpenseRow(Expense expense) {
        String payerSummary = expense.getPayers().stream()
                .sorted(Comparator.comparing(payer -> payer.getUser().getFullName()))
                .map(payer -> payer.getUser().getFullName() + ": " + payer.getPaidAmount())
                .collect(Collectors.joining("; "));
        String participantSummary = expense.getShares().stream()
                .sorted(Comparator.comparing(share -> share.getUser().getFullName()))
                .map(share -> share.getUser().getFullName() + ": " + share.getShareAmount())
                .collect(Collectors.joining("; "));
        return new ReportExpenseRow(
                expense.getId(),
                expense.getExpenseDate(),
                expense.getTitle(),
                expense.getCategory() == null ? "Chưa phân loại" : expense.getCategory().getName(),
                expense.getTotalAmount(),
                payerSummary,
                participantSummary,
                expense.getCreatedBy().getFullName(),
                expense.getDescription()
        );
    }

    private ReportSettlementRow toSettlementRow(Settlement settlement) {
        return new ReportSettlementRow(
                settlement.getId(),
                settlement.getSettledAt(),
                settlement.getPayer().getFullName(),
                settlement.getReceiver().getFullName(),
                settlement.getAmount(),
                settlement.getStatus().name(),
                settlement.getNote(),
                settlement.getCreatedBy().getFullName()
        );
    }

    private DateRange resolveRange(LocalDate requestedFrom, LocalDate requestedTo) {
        LocalDate today = LocalDate.now();
        LocalDate to = requestedTo == null ? today : requestedTo;
        LocalDate from = requestedFrom == null
                ? to.withDayOfMonth(1).minusMonths(DEFAULT_MONTH_COUNT - 1L)
                : requestedFrom;
        if (from.isAfter(to)) {
            throw new BusinessException("INVALID_DATE_RANGE", "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }
        long monthCount = ChronoUnit.MONTHS.between(YearMonth.from(from), YearMonth.from(to));
        if (monthCount >= MAX_MONTH_RANGE) {
            throw new BusinessException(
                    "REPORT_RANGE_TOO_LARGE",
                    "Báo cáo chỉ hỗ trợ tối đa " + MAX_MONTH_RANGE + " tháng"
            );
        }
        return new DateRange(from, to);
    }

    private BigDecimal percentage(long amount, long total) {
        if (total <= 0L) return BigDecimal.ZERO.setScale(2);
        return BigDecimal.valueOf(amount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private long safeAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            throw new BusinessException("AMOUNT_TOO_LARGE", "Số tiền vượt quá giới hạn hỗ trợ");
        }
    }

    private record DateRange(LocalDate from, LocalDate to) {}

    private static final class CategoryAccumulator {
        private final Long categoryId;
        private final String categoryName;
        private final String icon;
        private long amount;
        private int expenseCount;

        private CategoryAccumulator(Long categoryId, String categoryName, String icon) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
            this.icon = icon;
        }
    }

    private static final class MemberAccumulator {
        private final Long userId;
        private final String fullName;
        private final String email;
        private final String avatarUrl;
        private final String membershipStatus;
        private long paidAmount;
        private long shareAmount;

        private MemberAccumulator(GroupMember member) {
            this.userId = member.getUser().getId();
            this.fullName = member.getUser().getFullName();
            this.email = member.getUser().getEmail();
            this.avatarUrl = member.getUser().getAvatarUrl();
            this.membershipStatus = member.getStatus().name();
        }

        private MemberAccumulator(User user) {
            this.userId = user.getId();
            this.fullName = user.getFullName();
            this.email = user.getEmail();
            this.avatarUrl = user.getAvatarUrl();
            this.membershipStatus = "INACTIVE";
        }
    }

    private static final class MonthlyAccumulator {
        private long amount;
        private int expenseCount;
    }
}
