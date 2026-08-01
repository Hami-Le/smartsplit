package com.smartsplit.ocr.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.color.ColorSpace;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Creates an enlarged, grayscale, contrast-normalized PNG for OCR.
 * If Java ImageIO cannot decode the source (for example some WebP builds), the
 * original file is returned and Tesseract/Leptonica handles it directly.
 */
@Component
public class ReceiptImagePreprocessor {
    private final boolean enabled;
    private final int targetWidth;

    public ReceiptImagePreprocessor(
            @Value("${app.ocr.tesseract.preprocess:true}") boolean enabled,
            @Value("${app.ocr.tesseract.target-width:1800}") int targetWidth
    ) {
        this.enabled = enabled;
        this.targetWidth = Math.max(900, Math.min(targetWidth, 3200));
    }

    public PreparedReceiptImage prepare(Path source) {
        if (!enabled) {
            return new PreparedReceiptImage(source, false);
        }

        try {
            BufferedImage original = ImageIO.read(source.toFile());
            if (original == null || original.getWidth() <= 0 || original.getHeight() <= 0) {
                return new PreparedReceiptImage(source, false);
            }

            double scale = original.getWidth() < targetWidth
                    ? (double) targetWidth / original.getWidth()
                    : 1.0d;
            int width = Math.max(1, (int) Math.round(original.getWidth() * scale));
            int height = Math.max(1, (int) Math.round(original.getHeight() * scale));

            BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);
            AffineTransformOp resizeOperation = new AffineTransformOp(
                    transform,
                    AffineTransformOp.TYPE_BICUBIC
            );
            resizeOperation.filter(original, resized);

            BufferedImage grayscale = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null)
                    .filter(resized, grayscale);
            normalizeContrast(grayscale.getRaster());

            Path temporary = Files.createTempFile("smartsplit-receipt-", ".png");
            if (!ImageIO.write(grayscale, "png", temporary.toFile())) {
                Files.deleteIfExists(temporary);
                return new PreparedReceiptImage(source, false);
            }
            return new PreparedReceiptImage(temporary, true);
        } catch (IOException | RuntimeException exception) {
            return new PreparedReceiptImage(source, false);
        }
    }

    private void normalizeContrast(WritableRaster raster) {
        int[] histogram = new int[256];
        int width = raster.getWidth();
        int height = raster.getHeight();
        long total = (long) width * height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                histogram[raster.getSample(x, y, 0)]++;
            }
        }

        int low = percentile(histogram, total, 0.01d);
        int high = percentile(histogram, total, 0.99d);
        if (high <= low + 10) {
            return;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = raster.getSample(x, y, 0);
                int normalized = (value - low) * 255 / (high - low);
                raster.setSample(x, y, 0, Math.max(0, Math.min(255, normalized)));
            }
        }
    }

    private int percentile(int[] histogram, long total, double fraction) {
        long threshold = Math.max(1L, Math.round(total * fraction));
        long cumulative = 0L;
        for (int value = 0; value < histogram.length; value++) {
            cumulative += histogram[value];
            if (cumulative >= threshold) {
                return value;
            }
        }
        return 255;
    }
}
