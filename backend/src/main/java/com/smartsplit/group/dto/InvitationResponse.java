package com.smartsplit.group.dto;

import java.time.LocalDateTime;

public record InvitationResponse(
        Long id,
        Long groupId,
        String groupName,
        String email,
        String status,
        LocalDateTime expiresAt,
        String token,
        String invitationPath
) {}
