package com.smartsplit.auth.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserSummary user
) {
    public record UserSummary(Long id, String fullName, String email, String avatarUrl, String role) {}
}
