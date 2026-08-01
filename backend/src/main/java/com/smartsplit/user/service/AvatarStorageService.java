package com.smartsplit.user.service;

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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AvatarStorageService {
    private static final String PUBLIC_PATH = "/api/users/avatars/";
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final Path rootDirectory;
    private final long maxFileBytes;

    public AvatarStorageService(
            @Value("${app.avatar.storage-directory}") String storageDirectory,
            @Value("${app.avatar.max-file-bytes}") long maxFileBytes
    ) {
        this.rootDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
        this.maxFileBytes = maxFileBytes;
    }

    public String store(MultipartFile file) {
        validate(file);
        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        String fileName = UUID.randomUUID() + EXTENSIONS.get(contentType);
        Path destination = resolve(fileName);
        try {
            Files.createDirectories(rootDirectory);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return PUBLIC_PATH + fileName;
        } catch (IOException exception) {
            throw new BusinessException(
                    "AVATAR_STORAGE_FAILED",
                    "Không thể lưu ảnh đại diện",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    public Resource load(String fileName) {
        Path path = resolve(fileName);
        if (!Files.isRegularFile(path)) {
            throw new BusinessException("AVATAR_NOT_FOUND", "Không tìm thấy ảnh đại diện", HttpStatus.NOT_FOUND);
        }
        try {
            return new UrlResource(path.toUri());
        } catch (MalformedURLException exception) {
            throw new BusinessException("AVATAR_NOT_FOUND", "Không tìm thấy ảnh đại diện", HttpStatus.NOT_FOUND);
        }
    }

    public String contentType(String fileName) {
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        if (lowerName.endsWith(".png")) return "image/png";
        if (lowerName.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    public void delete(String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.startsWith(PUBLIC_PATH)) return;
        String fileName = avatarUrl.substring(PUBLIC_PATH.length());
        try {
            Files.deleteIfExists(resolve(fileName));
        } catch (IOException ignored) {
            // File cleanup must not make a successful profile update fail.
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("EMPTY_AVATAR_FILE", "Hãy chọn một ảnh đại diện");
        }
        if (file.getSize() > maxFileBytes) {
            throw new BusinessException(
                    "AVATAR_FILE_TOO_LARGE",
                    "Ảnh đại diện không được vượt quá 2 MB",
                    HttpStatus.PAYLOAD_TOO_LARGE
            );
        }
        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException("UNSUPPORTED_AVATAR_FILE", "Chỉ hỗ trợ ảnh JPG, PNG hoặc WebP");
        }
        try {
            if (!hasValidSignature(file.getBytes(), contentType)) {
                throw new BusinessException("INVALID_AVATAR_FILE", "Tệp đã chọn không phải là ảnh hợp lệ");
            }
        } catch (IOException exception) {
            throw new BusinessException(
                    "AVATAR_READ_FAILED",
                    "Không thể đọc ảnh đại diện",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    private boolean hasValidSignature(byte[] bytes, String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> bytes.length >= 3
                    && (bytes[0] & 0xff) == 0xff
                    && (bytes[1] & 0xff) == 0xd8
                    && (bytes[2] & 0xff) == 0xff;
            case "image/png" -> bytes.length >= 8
                    && (bytes[0] & 0xff) == 0x89
                    && bytes[1] == 0x50
                    && bytes[2] == 0x4e
                    && bytes[3] == 0x47
                    && bytes[4] == 0x0d
                    && bytes[5] == 0x0a
                    && bytes[6] == 0x1a
                    && bytes[7] == 0x0a;
            case "image/webp" -> bytes.length >= 12
                    && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
    }

    private Path resolve(String fileName) {
        if (fileName == null || !fileName.matches("[a-f0-9-]+\\.(jpg|png|webp)")) {
            throw new BusinessException("INVALID_AVATAR_PATH", "Đường dẫn ảnh đại diện không hợp lệ");
        }
        Path path = rootDirectory.resolve(fileName).normalize();
        if (!path.startsWith(rootDirectory)) {
            throw new BusinessException("INVALID_AVATAR_PATH", "Đường dẫn ảnh đại diện không hợp lệ");
        }
        return path;
    }
}
