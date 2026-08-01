package com.smartsplit.balance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BalanceEntry(
        @NotNull Long memberId,
        @NotBlank String memberName,
        long balance
) {}
