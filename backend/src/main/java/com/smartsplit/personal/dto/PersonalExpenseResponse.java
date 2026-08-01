package com.smartsplit.personal.dto;

import com.smartsplit.expense.dto.CategoryResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PersonalExpenseResponse(
        Long id,
        String title,
        Long amount,
        LocalDate expenseDate,
        CategoryResponse category,
        String note,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
