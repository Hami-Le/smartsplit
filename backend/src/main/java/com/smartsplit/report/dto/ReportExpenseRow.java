package com.smartsplit.report.dto;

import java.time.LocalDate;

public record ReportExpenseRow(
        Long id,
        LocalDate expenseDate,
        String title,
        String categoryName,
        long totalAmount,
        String payerSummary,
        String participantSummary,
        String createdByName,
        String description
) {}
