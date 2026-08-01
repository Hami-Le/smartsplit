package com.smartsplit.settlement.dto;

import java.time.LocalDateTime;

public record SettlementResponse(
        Long id,
        Long groupId,
        Long payerId,
        String payerName,
        Long receiverId,
        String receiverName,
        long amount,
        String note,
        String status,
        LocalDateTime settledAt,
        Long createdById,
        String createdByName,
        LocalDateTime createdAt
) {}
