package com.smartsplit.personal.service;

import com.smartsplit.common.exception.BusinessException;
import com.smartsplit.expense.dto.CategoryResponse;
import com.smartsplit.expense.entity.Category;
import com.smartsplit.expense.repository.CategoryRepository;
import com.smartsplit.personal.dto.*;
import com.smartsplit.personal.entity.MonthlyBudget;
import com.smartsplit.personal.entity.PersonalExpense;
import com.smartsplit.personal.repository.MonthlyBudgetRepository;
import com.smartsplit.personal.repository.PersonalExpenseRepository;
import com.smartsplit.user.entity.User;
import com.smartsplit.user.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PersonalFinanceService {
    private final PersonalExpenseRepository expenseRepository;
    private final MonthlyBudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;

    public PersonalFinanceService(
            PersonalExpenseRepository expenseRepository,
            MonthlyBudgetRepository budgetRepository,
            CategoryRepository categoryRepository,
            CurrentUserService currentUserService
    ) {
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public PersonalFinanceSummaryResponse getSummary(String requestedMonth) {
        User user = currentUserService.getRequiredUser();
        YearMonth month = parseMonth(requestedMonth);
        List<PersonalExpense> expenses = expenseRepository
                .findAllByUser_IdAndExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(
                        user.getId(),
                        month.atDay(1),
                        month.atEndOfMonth()
                );
        long totalSpent = expenses.stream()
                .mapToLong(PersonalExpense::getAmount)
                .reduce(0L, this::safeAdd);
        long budgetAmount = budgetRepository
                .findByUser_IdAndBudgetMonth(user.getId(), month.atDay(1))
                .map(MonthlyBudget::getAmount)
                .orElse(0L);
        long remainingAmount = budgetAmount > 0 ? safeSubtract(budgetAmount, totalSpent) : 0L;

        return new PersonalFinanceSummaryResponse(
                month.toString(),
                budgetAmount,
                totalSpent,
                remainingAmount,
                percentage(totalSpent, budgetAmount),
                budgetAmount > 0 && totalSpent > budgetAmount,
                expenses.size(),
                categoryBreakdown(expenses, totalSpent),
                expenses.stream().map(this::toResponse).toList()
        );
    }

    @Transactional
    public PersonalExpenseResponse create(UpsertPersonalExpenseRequest request) {
        User user = currentUserService.getRequiredUser();
        PersonalExpense expense = new PersonalExpense();
        expense.setUser(user);
        applyRequest(expense, request, user);
        return toResponse(expenseRepository.saveAndFlush(expense));
    }

    @Transactional
    public PersonalExpenseResponse update(Long expenseId, UpsertPersonalExpenseRequest request) {
        User user = currentUserService.getRequiredUser();
        PersonalExpense expense = requireOwnedExpense(expenseId, user.getId());
        applyRequest(expense, request, user);
        return toResponse(expenseRepository.saveAndFlush(expense));
    }

    @Transactional
    public void delete(Long expenseId) {
        User user = currentUserService.getRequiredUser();
        expenseRepository.delete(requireOwnedExpense(expenseId, user.getId()));
    }

    @Transactional
    public PersonalFinanceSummaryResponse setBudget(String requestedMonth, MonthlyBudgetRequest request) {
        User user = currentUserService.getRequiredUser();
        YearMonth month = parseMonth(requestedMonth);
        MonthlyBudget budget = budgetRepository
                .findByUser_IdAndBudgetMonth(user.getId(), month.atDay(1))
                .orElseGet(() -> {
                    MonthlyBudget created = new MonthlyBudget();
                    created.setUser(user);
                    created.setBudgetMonth(month.atDay(1));
                    return created;
                });
        budget.setAmount(request.amount());
        budgetRepository.saveAndFlush(budget);
        return getSummary(month.toString());
    }

    @Transactional
    public PersonalFinanceSummaryResponse deleteBudget(String requestedMonth) {
        User user = currentUserService.getRequiredUser();
        YearMonth month = parseMonth(requestedMonth);
        budgetRepository.findByUser_IdAndBudgetMonth(user.getId(), month.atDay(1))
                .ifPresent(budgetRepository::delete);
        budgetRepository.flush();
        return getSummary(month.toString());
    }

    private void applyRequest(
            PersonalExpense expense,
            UpsertPersonalExpenseRequest request,
            User user
    ) {
        expense.setTitle(request.title().trim());
        expense.setAmount(request.amount());
        expense.setExpenseDate(request.expenseDate());
        expense.setNote(normalizeNullable(request.note()));
        expense.setCategory(resolveCategory(request.categoryId(), user));
    }

    private Category resolveCategory(Long categoryId, User user) {
        if (categoryId == null) return null;
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(
                        "CATEGORY_NOT_FOUND",
                        "Không tìm thấy danh mục",
                        HttpStatus.NOT_FOUND
                ));
        if (!category.isSystem() && (
                category.getCreatedBy() == null
                        || !category.getCreatedBy().getId().equals(user.getId())
        )) {
            throw new BusinessException(
                    "CATEGORY_ACCESS_DENIED",
                    "Bạn không có quyền sử dụng danh mục này",
                    HttpStatus.FORBIDDEN
            );
        }
        return category;
    }

    private PersonalExpense requireOwnedExpense(Long expenseId, Long userId) {
        return expenseRepository.findByIdAndUser_Id(expenseId, userId)
                .orElseThrow(() -> new BusinessException(
                        "PERSONAL_EXPENSE_NOT_FOUND",
                        "Không tìm thấy khoản chi cá nhân",
                        HttpStatus.NOT_FOUND
                ));
    }

    private YearMonth parseMonth(String value) {
        if (value == null || value.isBlank()) return YearMonth.now();
        try {
            return YearMonth.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new BusinessException(
                    "INVALID_MONTH",
                    "Tháng phải có định dạng yyyy-MM"
            );
        }
    }

    private List<PersonalCategorySpendingResponse> categoryBreakdown(
            List<PersonalExpense> expenses,
            long totalSpent
    ) {
        record CategoryTotal(Long id, String name, String icon, long amount, long count) {
            CategoryTotal add(long value) {
                return new CategoryTotal(id, name, icon, Math.addExact(amount, value), count + 1);
            }
        }

        Map<String, CategoryTotal> totals = new LinkedHashMap<>();
        expenses.forEach(expense -> {
            Category category = expense.getCategory();
            Long id = category == null ? null : category.getId();
            String key = id == null ? "uncategorized" : id.toString();
            String name = category == null ? "Chưa phân loại" : category.getName();
            String icon = category == null ? null : category.getIcon();
            totals.compute(key, (ignored, current) -> current == null
                    ? new CategoryTotal(id, name, icon, expense.getAmount(), 1)
                    : current.add(expense.getAmount()));
        });

        return totals.values().stream()
                .sorted((left, right) -> Long.compare(right.amount(), left.amount()))
                .map(item -> new PersonalCategorySpendingResponse(
                        item.id(),
                        item.name(),
                        item.icon(),
                        item.amount(),
                        item.count(),
                        percentage(item.amount(), totalSpent)
                ))
                .toList();
    }

    private PersonalExpenseResponse toResponse(PersonalExpense expense) {
        Category category = expense.getCategory();
        CategoryResponse categoryResponse = category == null ? null : new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getIcon()
        );
        return new PersonalExpenseResponse(
                expense.getId(),
                expense.getTitle(),
                expense.getAmount(),
                expense.getExpenseDate(),
                categoryResponse,
                expense.getNote(),
                expense.getVersion(),
                expense.getCreatedAt(),
                expense.getUpdatedAt()
        );
    }

    private double percentage(long value, long total) {
        if (total <= 0) return 0D;
        return BigDecimal.valueOf(value)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private long safeAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            throw new BusinessException("AMOUNT_OVERFLOW", "Tổng số tiền vượt quá giới hạn hỗ trợ");
        }
    }

    private long safeSubtract(long left, long right) {
        try {
            return Math.subtractExact(left, right);
        } catch (ArithmeticException exception) {
            throw new BusinessException("AMOUNT_OVERFLOW", "Tổng số tiền vượt quá giới hạn hỗ trợ");
        }
    }

    private String normalizeNullable(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
