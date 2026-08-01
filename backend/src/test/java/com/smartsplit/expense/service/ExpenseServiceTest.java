package com.smartsplit.expense.service;

import com.smartsplit.common.exception.BusinessException;
import com.smartsplit.expense.dto.*;
import com.smartsplit.expense.entity.Expense;
import com.smartsplit.expense.entity.SplitType;
import com.smartsplit.expense.repository.CategoryRepository;
import com.smartsplit.expense.repository.ExpenseRepository;
import com.smartsplit.group.entity.*;
import com.smartsplit.group.repository.ExpenseGroupRepository;
import com.smartsplit.group.repository.GroupMemberRepository;
import com.smartsplit.user.entity.User;
import com.smartsplit.user.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpenseServiceTest {
    @Mock ExpenseRepository expenseRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock ExpenseGroupRepository groupRepository;
    @Mock GroupMemberRepository memberRepository;
    @Mock CurrentUserService currentUserService;

    private ExpenseService service;
    private ExpenseGroup group;
    private User ha;
    private User minh;
    private User lan;

    @BeforeEach
    void setUp() {
        service = new ExpenseService(
                expenseRepository,
                categoryRepository,
                groupRepository,
                memberRepository,
                currentUserService
        );
        group = mock(ExpenseGroup.class);
        when(group.getId()).thenReturn(10L);
        when(group.getName()).thenReturn("Du lịch Đà Lạt");

        ha = user(1L, "Hà", "ha@example.com");
        minh = user(2L, "Minh", "minh@example.com");
        lan = user(3L, "Lan", "lan@example.com");
    }

    @Test
    void createEqualSplitDistributesRemainderWithoutLosingMoney() {
        arrangeMemberships();
        when(expenseRepository.saveAndFlush(any(Expense.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpsertExpenseRequest request = new UpsertExpenseRequest(
                "Ăn sáng",
                null,
                100_000L,
                LocalDate.of(2026, 7, 29),
                null,
                List.of(new ExpensePayerInput(1L, 100_000L)),
                new ExpenseSplitInput(
                        SplitType.EQUAL,
                        List.of(
                                new ExpenseSplitParticipantInput(1L, null, null),
                                new ExpenseSplitParticipantInput(2L, null, null),
                                new ExpenseSplitParticipantInput(3L, null, null)
                        )
                )
        );

        service.create(10L, request);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).saveAndFlush(captor.capture());
        Expense saved = captor.getValue();
        assertThat(saved.getShares())
                .extracting(share -> share.getShareAmount())
                .containsExactly(33_334L, 33_333L, 33_333L);
        assertThat(saved.getShares().stream().mapToLong(share -> share.getShareAmount()).sum())
                .isEqualTo(100_000L);
    }

    @Test
    void rejectsWhenPayerTotalDoesNotMatchExpenseTotal() {
        arrangeMemberships();
        UpsertExpenseRequest request = new UpsertExpenseRequest(
                "Khách sạn",
                null,
                2_300_000L,
                LocalDate.now(),
                null,
                List.of(new ExpensePayerInput(1L, 2_000_000L)),
                new ExpenseSplitInput(
                        SplitType.EQUAL,
                        List.of(new ExpenseSplitParticipantInput(1L, null, null))
                )
        );

        assertThatThrownBy(() -> service.create(10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Tổng tiền những người đã trả");
        verify(expenseRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsPercentageSplitWhoseTotalIsNotOneHundred() {
        arrangeMemberships();
        UpsertExpenseRequest request = new UpsertExpenseRequest(
                "BBQ",
                null,
                300_000L,
                LocalDate.now(),
                null,
                List.of(new ExpensePayerInput(1L, 300_000L)),
                new ExpenseSplitInput(
                        SplitType.PERCENTAGE,
                        List.of(
                                new ExpenseSplitParticipantInput(1L, null, java.math.BigDecimal.valueOf(40)),
                                new ExpenseSplitParticipantInput(2L, null, java.math.BigDecimal.valueOf(50))
                        )
                )
        );

        assertThatThrownBy(() -> service.create(10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("100%");
    }

    private void arrangeMemberships() {
        when(currentUserService.getRequiredUser()).thenReturn(ha);
        when(groupRepository.findByIdAndStatus(10L, GroupStatus.ACTIVE))
                .thenReturn(Optional.of(group));
        when(memberRepository.findByGroup_IdAndUser_IdAndStatus(
                10L,
                1L,
                GroupMemberStatus.ACTIVE
        )).thenReturn(Optional.of(member(ha, GroupRole.OWNER)));
        when(memberRepository.findAllByGroup_IdAndStatusOrderByJoinedAtAsc(
                10L,
                GroupMemberStatus.ACTIVE
        )).thenReturn(List.of(
                member(ha, GroupRole.OWNER),
                member(minh, GroupRole.MEMBER),
                member(lan, GroupRole.MEMBER)
        ));
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
