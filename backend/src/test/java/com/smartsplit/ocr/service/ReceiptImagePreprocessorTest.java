package com.smartsplit.ocr.service;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiptImagePreprocessorTest {
    @Test
    void createsAndDeletesTemporaryPng() throws Exception {
        Path source = Files.createTempFile("receipt-source-", ".png");
        try {
            BufferedImage image = new BufferedImage(400, 600, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    image.setRGB(x, y, (x + y) % 5 == 0 ? 0x111111 : 0xFFFFFF);
                }
            }
            ImageIO.write(image, "png", source.toFile());

            ReceiptImagePreprocessor preprocessor = new ReceiptImagePreprocessor(true, 1800);
            Path preparedPath;
            try (PreparedReceiptImage prepared = preprocessor.prepare(source)) {
                preparedPath = prepared.path();
                assertTrue(prepared.temporary());
                assertTrue(Files.exists(preparedPath));
                assertTrue(ImageIO.read(preparedPath.toFile()).getWidth() >= 1800);
            }
            assertFalse(Files.exists(preparedPath));
        } finally {
            Files.deleteIfExists(source);
        }
    }
}
