package com.smartsplit.ocr.service;

import java.nio.file.Path;

/**
 * Contract for OCR engines that can extract text from a locally stored receipt image.
 */
public interface ReceiptOcrClient {
    boolean isConfigured();

    String configurationMessage();

    OcrTextResult extractText(Path imagePath);
}
