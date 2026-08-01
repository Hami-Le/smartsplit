package com.smartsplit.ocr.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReceiptScanResponse(
        Long id,
        Long groupId,
        String status,
        String provider,
        String originalFileName,
        boolean hasFile,
        String merchant,
        Long totalAmount,
        LocalDate expenseDate,
        Long categoryId,
        String categoryName,
        Double confidence,
        String rawText,
        String message,
        LocalDateTime createdAt
) {
}
