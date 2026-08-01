package com.smartsplit.common.exception;

import java.time.OffsetDateTime;
import java.util.Map;

public record ApiError(
        boolean success,
        String code,
        String message,
        Map<String, String> errors,
        OffsetDateTime timestamp
) {
    public static ApiError of(String code, String message, Map<String, String> errors) {
        return new ApiError(false, code, message, errors, OffsetDateTime.now());
    }
}
