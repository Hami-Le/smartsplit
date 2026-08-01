package com.smartsplit.balance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SimplifyDebtRequest(
        @NotEmpty(message = "Danh sách số dư không được để trống")
        List<@Valid BalanceEntry> balances
) {}
