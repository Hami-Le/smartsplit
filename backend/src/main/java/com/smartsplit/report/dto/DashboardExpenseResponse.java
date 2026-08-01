package com.smartsplit.report.dto;

import java.time.LocalDate;

public record DashboardExpenseResponse(
        Long id,
        String title,
        LocalDate expenseDate,
        long totalAmount,
        String categoryName,
        String categoryIcon,
        String createdByName
) {}
