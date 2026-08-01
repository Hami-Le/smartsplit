package com.smartsplit.group.dto;

import java.time.LocalDateTime;

public record GroupSummaryResponse(
        Long id,
        String name,
        String description,
        String avatarUrl,
        String defaultCurrency,
        String currentUserRole,
        long memberCount,
        LocalDateTime createdAt
) {}
