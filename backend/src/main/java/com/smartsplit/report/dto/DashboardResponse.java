package com.smartsplit.report.dto;

import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
        Long groupId,
        String groupName,
        String currency,
        String currentUserRole,
        LocalDate from,
        LocalDate to,
        long totalExpense,
        int expenseCount,
        long averageExpense,
        long highestExpense,
        long totalSettled,
        long outstandingAmount,
        DashboardExpenseResponse largestExpense,
        List<CategorySpendingResponse> categoryBreakdown,
        List<MemberSpendingResponse> memberSpending,
        List<MonthlySpendingResponse> monthlyTrend,
        List<DashboardExpenseResponse> recentExpenses
) {}
