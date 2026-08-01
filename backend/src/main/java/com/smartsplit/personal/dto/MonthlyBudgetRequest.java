package com.smartsplit.personal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MonthlyBudgetRequest(
        @NotNull(message = "Ngân sách không được để trống")
        @Positive(message = "Ngân sách phải lớn hơn 0")
        Long amount
) {}
