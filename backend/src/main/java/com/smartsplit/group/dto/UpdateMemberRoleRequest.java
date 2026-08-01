package com.smartsplit.group.dto;

import com.smartsplit.group.entity.GroupRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull(message = "Vai trò không được để trống")
        GroupRole role
) {}
