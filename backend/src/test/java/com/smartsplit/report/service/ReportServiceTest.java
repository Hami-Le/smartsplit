package com.smartsplit.report.service;

import com.smartsplit.balance.dto.GroupBalanceResponse;
import com.smartsplit.balance.dto.MemberBalanceResponse;
import com.smartsplit.balance.service.GroupBalanceService;
import com.smartsplit.expense.entity.*;
import com.smartsplit.expense.repository.ExpenseRepository;
import com.smartsplit.group.entity.*;
import com.smartsplit.group.repository.ExpenseGroupRepository;
import com.smartsplit.group.repository.GroupMemberRepository;
import com.smartsplit.report.dto.DashboardResponse;
import com.smartsplit.settlement.entity.Settlement;
import com.smartsplit.settlement.entity.SettlementStatus;
import com.smartsplit.settlement.repository.SettlementRepository;
import com.smartsplit.user.entity.User;
import com.smartsplit.user.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceTest {
    @Mock ExpenseGroupRepository groupRepository;
    @Mock GroupMemberRepository memberRepository;
    @Mock ExpenseRepository expenseRepository;
    @Mock SettlementRepository settlementRepository;
    @Mock CurrentUserService currentUserService;
    @Mock GroupBalanceService groupBalanceService;

    private ReportService service;
    private ExpenseGroup group;
    private User ha;
    private User minh;

    @BeforeEach
    void setUp() {
        service = new ReportService(
                groupRepository,
                memberRepository,
                expenseRepository,
                settlementRepository,
                currentUserService,
                groupBalanceService
        );
        group = mock(ExpenseGroup.class);
        when(group.getId()).thenReturn(10L);
        when(group.getName()).thenReturn("Du lịch Đà Lạt");
        when(group.getDefaultCurrency()).thenReturn("VND");
        ha = user(1L, "Hà", "ha@example.com");
        minh = user(2L, "Minh", "minh@example.com");
    }

    @Test
    void aggregatesDashboardByCategoryMemberAndMonth() {
        GroupMember haMember = member(ha, GroupRole.OWNER);
        GroupMember minhMember = member(minh, GroupRole.MEMBER);
        when(currentUserService.getRequiredUser()).thenReturn(ha);
        when(groupRepository.findByIdAndStatus(10L, GroupStatus.ACTIVE)).thenReturn(Optional.of(group));
        when(memberRepository.findByGroup_IdAndUser_IdAndStatus(10L, 1L, GroupMemberStatus.ACTIVE))
                .thenReturn(Optional.of(haMember));
        when(memberRepository.findAllByGroup_IdOrderByJoinedAtAsc(10L))
                .thenReturn(List.of(haMember, minhMember));

        Category food = mock(Category.class);
        when(food.getId()).thenReturn(1L);
        when(food.getName()).thenReturn("Ăn uống");
        when(food.getIcon()).thenReturn("🍜");

        Expense july = expense(
                101L,
                "BBQ",
                600_000L,
                LocalDate.of(2026, 7, 10),
                food,
                List.of(payer(ha, 600_000L)),
                List.of(share(ha, 300_000L), share(minh, 300_000L))
        );
        Expense august = expense(
                102L,
                "Taxi",
                200_000L,
                LocalDate.of(2026, 8, 2),
                null,
                List.of(payer(minh, 200_000L)),
                List.of(share(ha, 100_000L), share(minh, 100_000L))
        );
        when(expenseRepository.findAllByGroup_IdAndStatusOrderByExpenseDateDescCreatedAtDesc(
                10L, ExpenseStatus.ACTIVE
        )).thenReturn(List.of(august, july));

        Settlement settlement = mock(Settlement.class);
        when(settlement.getStatus()).thenReturn(SettlementStatus.CONFIRMED);
        when(settlement.getAmount()).thenReturn(100_000L);
        when(settlement.getSettledAt()).thenReturn(LocalDateTime.of(2026, 8, 3, 10, 0));
        when(settlement.getPayer()).thenReturn(minh);
        when(settlement.getReceiver()).thenReturn(ha);
        when(settlement.getCreatedBy()).thenReturn(minh);
        when(settlement.getId()).thenReturn(1L);
        when(settlementRepository.findAllByGroup_IdOrderBySettledAtDescCreatedAtDesc(10L))
                .thenReturn(List.of(settlement));

        when(groupBalanceService.calculateForAuthorizedService(10L)).thenReturn(new GroupBalanceResponse(
                10L,
                "Du lịch Đà Lạt",
                "VND",
                null,
                800_000L,
                100_000L,
                List.of(
                        new MemberBalanceResponse(1L, "Hà", "ha@example.com", null, "ACTIVE", 600_000L, 400_000L, 0L, 100_000L, 100_000L),
                        new MemberBalanceResponse(2L, "Minh", "minh@example.com", null, "ACTIVE", 200_000L, 400_000L, 100_000L, 0L, -100_000L)
                ),
                List.of()
        ));

        DashboardResponse result = service.getDashboard(
                10L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 8, 31)
        );

        assertThat(result.totalExpense()).isEqualTo(800_000L);
        assertThat(result.expenseCount()).isEqualTo(2);
        assertThat(result.averageExpense()).isEqualTo(400_000L);
        assertThat(result.highestExpense()).isEqualTo(600_000L);
        assertThat(result.totalSettled()).isEqualTo(100_000L);
        assertThat(result.outstandingAmount()).isEqualTo(100_000L);
        assertThat(result.categoryBreakdown())
                .extracting(category -> category.categoryName() + ":" + category.amount())
                .containsExactly("Ăn uống:600000", "Chưa phân loại:200000");
        assertThat(result.monthlyTrend())
                .extracting(month -> month.month() + ":" + month.amount())
                .containsExactly("2026-07:600000", "2026-08:200000");
        assertThat(result.memberSpending())
                .extracting(member -> member.fullName() + ":" + member.shareAmount())
                .containsExactly("Hà:400000", "Minh:400000");
    }

    private Expense expense(
            Long id,
            String title,
            long amount,
            LocalDate date,
            Category category,
            List<ExpensePayer> payers,
            List<ExpenseShare> shares
    ) {
        Expense expense = mock(Expense.class);
        when(expense.getId()).thenReturn(id);
        when(expense.getTitle()).thenReturn(title);
        when(expense.getTotalAmount()).thenReturn(amount);
        when(expense.getExpenseDate()).thenReturn(date);
        when(expense.getCategory()).thenReturn(category);
        when(expense.getCreatedBy()).thenReturn(ha);
        when(expense.getPayers()).thenReturn(payers);
        when(expense.getShares()).thenReturn(shares);
        return expense;
    }

    private ExpensePayer payer(User user, long amount) {
        ExpensePayer payer = new ExpensePayer();
        payer.setUser(user);
        payer.setPaidAmount(amount);
        return payer;
    }

    private ExpenseShare share(User user, long amount) {
        ExpenseShare share = new ExpenseShare();
        share.setUser(user);
        share.setShareAmount(amount);
        share.setSplitType(SplitType.EQUAL);
        return share;
    }

    private GroupMember member(User user, GroupRole role) {
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        member.setRole(role);
        member.setStatus(GroupMemberStatus.ACTIVE);
        return member;
    }

    private User user(Long id, String name, String email) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getFullName()).thenReturn(name);
        when(user.getEmail()).thenReturn(email);
        return user;
    }
}
