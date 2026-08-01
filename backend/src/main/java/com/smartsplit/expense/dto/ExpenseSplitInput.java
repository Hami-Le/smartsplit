package com.smartsplit.expense.dto;

import com.smartsplit.expense.entity.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ExpenseSplitInput(
        @NotNull(message = "Cách chia tiền không được để trống")
        SplitType type,

        @NotEmpty(message = "Phải chọn ít nhất một người tham gia")
        List<@Valid ExpenseSplitParticipantInput> participants
) {}
