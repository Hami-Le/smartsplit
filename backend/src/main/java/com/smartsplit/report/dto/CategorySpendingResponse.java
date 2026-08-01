package com.smartsplit.report.dto;

import java.math.BigDecimal;

public record CategorySpendingResponse(
        Long categoryId,
        String categoryName,
        String icon,
        long amount,
        int expenseCount,
        BigDecimal percentage
) {}
