package com.smartsplit.group.dto;

import java.time.LocalDateTime;
import java.util.List;

public record GroupDetailResponse(
        Long id,
        String name,
        String description,
        String avatarUrl,
        String defaultCurrency,
        String currentUserRole,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<GroupMemberResponse> members
) {}
