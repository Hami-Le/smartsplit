package com.smartsplit.ocr.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record PreparedReceiptImage(Path path, boolean temporary) implements AutoCloseable {
    @Override
    public void close() {
        if (!temporary) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // The operating system can clean the temporary directory later.
        }
    }
}
