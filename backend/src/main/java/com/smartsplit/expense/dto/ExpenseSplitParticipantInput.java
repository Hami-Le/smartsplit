package com.smartsplit.expense.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ExpenseSplitParticipantInput(
        @NotNull(message = "Thành viên tham gia không được để trống")
        Long userId,
        Long amount,
        BigDecimal percentage
) {}
