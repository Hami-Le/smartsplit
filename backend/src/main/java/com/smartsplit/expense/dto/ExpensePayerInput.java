package com.smartsplit.expense.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ExpensePayerInput(
        @NotNull(message = "Người trả không được để trống")
        Long userId,

        @NotNull(message = "Số tiền đã trả không được để trống")
        @Positive(message = "Số tiền đã trả phải lớn hơn 0")
        Long amount
) {}
