package com.smartsplit.report.dto;

public record MonthlySpendingResponse(
        String month,
        String label,
        long amount,
        int expenseCount
) {}
