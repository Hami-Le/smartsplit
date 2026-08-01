package com.smartsplit.user.dto;

public record UserProfileResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        String avatarUrl,
        String role
) {}
