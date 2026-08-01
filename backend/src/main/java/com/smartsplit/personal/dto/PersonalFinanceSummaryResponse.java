package com.smartsplit.personal.dto;

import java.util.List;

public record PersonalFinanceSummaryResponse(
        String month,
        Long budgetAmount,
        Long totalSpent,
        Long remainingAmount,
        double usagePercentage,
        boolean overBudget,
        int expenseCount,
        List<PersonalCategorySpendingResponse> categoryBreakdown,
        List<PersonalExpenseResponse> expenses
) {}
