package com.smartsplit.group.dto;

import java.time.LocalDateTime;

public record GroupMemberResponse(
        Long membershipId,
        Long userId,
        String fullName,
        String email,
        String avatarUrl,
        String role,
        String status,
        LocalDateTime joinedAt
) {}
