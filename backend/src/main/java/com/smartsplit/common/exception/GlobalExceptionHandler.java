package com.smartsplit.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.badRequest()
                .body(ApiError.of("VALIDATION_ERROR", "Dữ liệu không hợp lệ", errors));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(ApiError.of(exception.getCode(), exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of("INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng", Map.of()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableMessage() {
        return ResponseEntity.badRequest()
                .body(ApiError.of(
                        "INVALID_REQUEST_BODY",
                        "Nội dung JSON, ngày tháng hoặc kiểu chia tiền không hợp lệ",
                        Map.of()
                ));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLock() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(
                        "CONCURRENT_UPDATE",
                        "Khoản chi vừa được thay đổi ở nơi khác. Hãy tải lại trước khi lưu.",
                        Map.of()
                ));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSize() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiError.of(
                        "RECEIPT_FILE_TOO_LARGE",
                        "Ảnh hóa đơn vượt quá giới hạn 5 MB",
                        Map.of()
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException exception) {
        log.warn("Vi phạm ràng buộc dữ liệu", exception);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(
                        "DATA_CONFLICT",
                        "Dữ liệu đang được sử dụng hoặc bị trùng lặp",
                        Map.of()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        log.error("Lỗi ngoài dự kiến khi xử lý API", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("INTERNAL_ERROR", "Hệ thống gặp lỗi ngoài dự kiến", Map.of()));
    }
}
