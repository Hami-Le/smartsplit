package com.smartsplit.ocr.service;

import com.smartsplit.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalReceiptStorage {
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final Path rootDirectory;
    private final long maxFileBytes;

    public LocalReceiptStorage(
            @Value("${app.ocr.storage-directory}") String storageDirectory,
            @Value("${app.ocr.max-file-bytes}") long maxFileBytes
    ) {
        this.rootDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
        this.maxFileBytes = maxFileBytes;
    }

    public StoredReceiptFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPTY_RECEIPT_FILE", "Hãy chọn một ảnh hóa đơn");
        }
        if (file.getSize() > maxFileBytes) {
            throw new BusinessException(
                    "RECEIPT_FILE_TOO_LARGE",
                    "Ảnh hóa đơn vượt quá giới hạn 5 MB",
                    HttpStatus.PAYLOAD_TOO_LARGE
            );
        }

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(
                    "UNSUPPORTED_RECEIPT_FILE",
                    "Chỉ hỗ trợ ảnh JPG, PNG hoặc WebP"
            );
        }

        String originalName = cleanOriginalName(file.getOriginalFilename());
        String extension = extensionFor(contentType);
        Path destination = rootDirectory.resolve(UUID.randomUUID() + extension).normalize();
        if (!destination.startsWith(rootDirectory)) {
            throw new BusinessException("INVALID_RECEIPT_PATH", "Đường dẫn tệp không hợp lệ");
        }

        try {
            Files.createDirectories(rootDirectory);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return new StoredReceiptFile(destination, originalName, contentType, file.getSize());
        } catch (IOException exception) {
            throw new BusinessException(
                    "RECEIPT_STORAGE_FAILED",
                    "Không thể lưu ảnh hóa đơn",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    public byte[] readBytes(String storagePath) {
        Path path = resolveExisting(storagePath);
        try {
            return Files.readAllBytes(path);
        } catch (IOException exception) {
            throw new BusinessException(
                    "RECEIPT_READ_FAILED",
                    "Không thể đọc ảnh hóa đơn",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    public Resource loadAsResource(String storagePath) {
        Path path = resolveExisting(storagePath);
        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException(
                        "RECEIPT_FILE_NOT_FOUND",
                        "Không tìm thấy ảnh hóa đơn",
                        HttpStatus.NOT_FOUND
                );
            }
            return resource;
        } catch (MalformedURLException exception) {
            throw new BusinessException(
                    "RECEIPT_FILE_NOT_FOUND",
                    "Không tìm thấy ảnh hóa đơn",
                    HttpStatus.NOT_FOUND
            );
        }
    }

    private Path resolveExisting(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new BusinessException(
                    "RECEIPT_FILE_NOT_FOUND",
                    "Phiên OCR này không có ảnh đính kèm",
                    HttpStatus.NOT_FOUND
            );
        }
        Path path = Path.of(storagePath).toAbsolutePath().normalize();
        if (!path.startsWith(rootDirectory) || !Files.exists(path)) {
            throw new BusinessException(
                    "RECEIPT_FILE_NOT_FOUND",
                    "Không tìm thấy ảnh hóa đơn",
                    HttpStatus.NOT_FOUND
            );
        }
        return path;
    }

    private String cleanOriginalName(String name) {
        if (name == null || name.isBlank()) return "receipt";
        String normalized = Path.of(name).getFileName().toString().replaceAll("[\\r\\n]", "_");
        return normalized.length() > 255 ? normalized.substring(normalized.length() - 255) : normalized;
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
