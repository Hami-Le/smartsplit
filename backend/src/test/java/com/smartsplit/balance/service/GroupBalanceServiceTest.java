package com.smartsplit.balance.service;

import com.smartsplit.balance.dto.GroupBalanceResponse;
import com.smartsplit.expense.entity.*;
import com.smartsplit.expense.repository.ExpenseRepository;
import com.smartsplit.group.entity.*;
import com.smartsplit.group.repository.ExpenseGroupRepository;
import com.smartsplit.group.repository.GroupMemberRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupBalanceServiceTest {
    @Mock ExpenseGroupRepository groupRepository;
    @Mock GroupMemberRepository memberRepository;
    @Mock ExpenseRepository expenseRepository;
    @Mock SettlementRepository settlementRepository;
    @Mock CurrentUserService currentUserService;

    private GroupBalanceService service;
    private ExpenseGroup group;
    private User ha;
    private User minh;
    private User lan;

    @BeforeEach
    void setUp() {
        service = new GroupBalanceService(
                groupRepository,
                memberRepository,
                expenseRepository,
                settlementRepository,
                currentUserService,
                new DebtSimplificationService()
        );
        group = mock(ExpenseGroup.class);
        when(group.getId()).thenReturn(10L);
        when(group.getName()).thenReturn("Du lịch Đà Lạt");
        when(group.getDefaultCurrency()).thenReturn("VND");
        ha = user(1L, "Hà", "ha@example.com");
        minh = user(2L, "Minh", "minh@example.com");
        lan = user(3L, "Lan", "lan@example.com");
    }

    @Test
    void calculatesBalancesFromExpensesAndConfirmedSettlements() {
        GroupMember haMember = member(ha, GroupRole.OWNER);
        GroupMember minhMember = member(minh, GroupRole.MEMBER);
        GroupMember lanMember = member(lan, GroupRole.MEMBER);

        when(currentUserService.getRequiredUser()).thenReturn(ha);
        when(groupRepository.findByIdAndStatus(10L, GroupStatus.ACTIVE))
                .thenReturn(Optional.of(group));
        when(memberRepository.findByGroup_IdAndUser_IdAndStatus(
                10L, 1L, GroupMemberStatus.ACTIVE
        )).thenReturn(Optional.of(haMember));
        when(memberRepository.findAllByGroup_IdOrderByJoinedAtAsc(10L))
                .thenReturn(List.of(haMember, minhMember, lanMember));

        Expense expense = mock(Expense.class);
        when(expense.getTotalAmount()).thenReturn(600_000L);
        when(expense.getPayers()).thenReturn(List.of(payer(ha, 600_000L)));
        when(expense.getShares()).thenReturn(List.of(
                share(ha, 200_000L),
                share(minh, 200_000L),
                share(lan, 200_000L)
        ));
        when(expenseRepository.findAllByGroup_IdAndStatusOrderByExpenseDateDescCreatedAtDesc(
                10L, ExpenseStatus.ACTIVE
        )).thenReturn(List.of(expense));

        Settlement settlement = mock(Settlement.class);
        when(settlement.getPayer()).thenReturn(minh);
        when(settlement.getReceiver()).thenReturn(ha);
        when(settlement.getAmount()).thenReturn(100_000L);
        when(settlementRepository.findAllByGroup_IdAndStatusOrderBySettledAtDescCreatedAtDesc(
                10L, SettlementStatus.CONFIRMED
        )).thenReturn(List.of(settlement));

        GroupBalanceResponse result = service.getBalances(10L);

        assertThat(result.totalExpense()).isEqualTo(600_000L);
        assertThat(result.totalSettled()).isEqualTo(100_000L);
        assertThat(result.members())
                .extracting(member -> member.fullName() + ":" + member.balance())
                .containsExactly("Hà:300000", "Minh:-100000", "Lan:-200000");
        assertThat(result.suggestedTransfers())
                .extracting(transfer -> transfer.amount())
                .containsExactlyInAnyOrder(100_000L, 200_000L);
    }

    private GroupMember member(User user, GroupRole role) {
        GroupMember member = new GroupMember();
        member.setGroup(group);
        member.setUser(user);
        member.setRole(role);
        member.setStatus(GroupMemberStatus.ACTIVE);
        return member;
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

    private User user(Long id, String name, String email) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        when(user.getFullName()).thenReturn(name);
        when(user.getEmail()).thenReturn(email);
        return user;
    }
}
