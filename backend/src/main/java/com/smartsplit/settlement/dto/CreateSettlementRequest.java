package com.smartsplit.settlement.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateSettlementRequest(
        @NotNull(message = "Người trả không được để trống") Long payerId,
        @NotNull(message = "Người nhận không được để trống") Long receiverId,
        @Positive(message = "Số tiền thanh toán phải lớn hơn 0") long amount,
        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự") String note,
        LocalDateTime settledAt
) {}
