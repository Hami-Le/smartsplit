package com.smartsplit.expense.dto;

import com.smartsplit.expense.entity.ExpenseStatus;
import com.smartsplit.expense.entity.SplitType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ExpenseResponse(
        Long id,
        Long groupId,
        String groupName,
        String title,
        String description,
        Long totalAmount,
        LocalDate expenseDate,
        CategoryResponse category,
        Long createdByUserId,
        String createdByName,
        ExpenseStatus status,
        SplitType splitType,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ExpensePersonAmountResponse> payers,
        List<ExpensePersonAmountResponse> shares
) {}
