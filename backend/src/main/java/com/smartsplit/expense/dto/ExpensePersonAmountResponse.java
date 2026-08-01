package com.smartsplit.expense.dto;

import java.math.BigDecimal;

public record ExpensePersonAmountResponse(
        Long userId,
        String fullName,
        String email,
        String avatarUrl,
        Long amount,
        BigDecimal percentage
) {}
