package com.smartsplit.ocr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParseReceiptTextRequest(
        @NotBlank(message = "Nội dung OCR không được để trống")
        @Size(max = 30000, message = "Nội dung OCR quá dài")
        String rawText
) {
}
