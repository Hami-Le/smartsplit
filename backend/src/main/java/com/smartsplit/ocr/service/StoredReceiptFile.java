package com.smartsplit.ocr.service;

import java.nio.file.Path;

public record StoredReceiptFile(
        Path path,
        String originalName,
        String contentType,
        long size
) {
}
