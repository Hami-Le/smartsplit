package com.smartsplit.group.dto;

public record AcceptInvitationResponse(
        Long groupId,
        String groupName,
        String role
) {}
