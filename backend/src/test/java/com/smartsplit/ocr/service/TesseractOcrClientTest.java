package com.smartsplit.ocr.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TesseractOcrClientTest {
    @Test
    void reportsDisabledConfigurationWithoutStartingAProcess() {
        ReceiptImagePreprocessor preprocessor = new ReceiptImagePreprocessor(false, 1800);
        TesseractOcrClient client = new TesseractOcrClient(
                false,
                "tesseract",
                "",
                "vie+eng",
                6,
                true,
                40,
                preprocessor
        );

        assertFalse(client.isConfigured());
        assertTrue(client.configurationMessage().contains("OCR đang tắt"));
    }
}
