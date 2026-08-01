package com.smartsplit.personal.service;

import com.smartsplit.common.exception.BusinessException;
import com.smartsplit.expense.entity.Category;
import com.smartsplit.expense.repository.CategoryRepository;
import com.smartsplit.personal.dto.PersonalFinanceSummaryResponse;
import com.smartsplit.personal.dto.UpsertPersonalExpenseRequest;
import com.smartsplit.personal.entity.MonthlyBudget;
import com.smartsplit.personal.entity.PersonalExpense;
import com.smartsplit.personal.repository.MonthlyBudgetRepository;
import com.smartsplit.personal.repository.PersonalExpenseRepository;
import com.smartsplit.user.entity.User;
import com.smartsplit.user.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalFinanceServiceTest {
    @Mock PersonalExpenseRepository expenseRepository;
    @Mock MonthlyBudgetRepository budgetRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock CurrentUserService currentUserService;

    private PersonalFinanceService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new PersonalFinanceService(
                expenseRepository,
                budgetRepository,
                categoryRepository,
                currentUserService
        );
        user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(currentUserService.getRequiredUser()).thenReturn(user);
    }

    @Test
    void summarizesBudgetAndCategoriesForRequestedMonth() {
        Category food = category(1L, "Ăn uống", "utensils");
        Category transport = category(2L, "Di chuyển", "car");
        List<PersonalExpense> expenses = List.of(
                expense(11L, "Ăn trưa", 70_000L, LocalDate.of(2026, 8, 2), food),
                expense(12L, "Ăn tối", 130_000L, LocalDate.of(2026, 8, 3), food),
                expense(13L, "Gửi xe", 20_000L, LocalDate.of(2026, 8, 3), transport)
        );
        MonthlyBudget budget = mock(MonthlyBudget.class);
        when(budget.getAmount()).thenReturn(500_000L);
        when(expenseRepository.findAllByUser_IdAndExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(
                7L,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31)
        )).thenReturn(expenses);
        when(budgetRepository.findByUser_IdAndBudgetMonth(7L, LocalDate.of(2026, 8, 1)))
                .thenReturn(Optional.of(budget));

        PersonalFinanceSummaryResponse result = service.getSummary("2026-08");

        assertThat(result.totalSpent()).isEqualTo(220_000L);
        assertThat(result.remainingAmount()).isEqualTo(280_000L);
        assertThat(result.usagePercentage()).isEqualTo(44.0);
        assertThat(result.overBudget()).isFalse();
        assertThat(result.categoryBreakdown())
                .extracting(item -> item.categoryName() + ":" + item.amount())
                .containsExactly("Ăn uống:200000", "Di chuyển:20000");
    }

    @Test
    void doesNotUpdateExpenseOwnedByAnotherUser() {
        when(expenseRepository.findByIdAndUser_Id(99L, 7L)).thenReturn(Optional.empty());
        UpsertPersonalExpenseRequest request = new UpsertPersonalExpenseRequest(
                "Khoản chi khác",
                50_000L,
                LocalDate.of(2026, 8, 2),
                null,
                null
        );

        assertThatThrownBy(() -> service.update(99L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không tìm thấy khoản chi cá nhân");
        verify(expenseRepository, never()).saveAndFlush(any());
    }

    private Category category(Long id, String name, String icon) {
        Category category = mock(Category.class);
        when(category.getId()).thenReturn(id);
        when(category.getName()).thenReturn(name);
        when(category.getIcon()).thenReturn(icon);
        return category;
    }

    private PersonalExpense expense(
            Long id,
            String title,
            long amount,
            LocalDate date,
            Category category
    ) {
        PersonalExpense expense = mock(PersonalExpense.class);
        when(expense.getId()).thenReturn(id);
        when(expense.getTitle()).thenReturn(title);
        when(expense.getAmount()).thenReturn(amount);
        when(expense.getExpenseDate()).thenReturn(date);
        when(expense.getCategory()).thenReturn(category);
        return expense;
    }
}
