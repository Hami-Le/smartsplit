package com.smartsplit.ocr.dto;

import java.time.LocalDateTime;

public record ReceiptAttachmentResponse(
        Long id,
        String fileUrl,
        String fileType,
        String ocrStatus,
        LocalDateTime createdAt
) {
}
