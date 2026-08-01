package com.smartsplit.report.dto;

import java.time.LocalDateTime;

public record ReportSettlementRow(
        Long id,
        LocalDateTime settledAt,
        String payerName,
        String receiverName,
        long amount,
        String status,
        String note,
        String createdByName
) {}
