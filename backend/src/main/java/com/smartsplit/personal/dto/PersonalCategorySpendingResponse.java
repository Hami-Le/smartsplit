package com.smartsplit.personal.dto;

public record PersonalCategorySpendingResponse(
        Long categoryId,
        String categoryName,
        String icon,
        Long amount,
        Long expenseCount,
        double percentage
) {}
