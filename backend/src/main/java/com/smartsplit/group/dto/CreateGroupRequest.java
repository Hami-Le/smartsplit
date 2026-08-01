package com.smartsplit.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
        @NotBlank(message = "Tên nhóm không được để trống")
        @Size(max = 150, message = "Tên nhóm không được vượt quá 150 ký tự")
        String name,

        @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
        String description,

        @Pattern(regexp = "^[A-Z]{3}$", message = "Mã tiền tệ phải gồm 3 chữ cái viết hoa")
        String defaultCurrency
) {}
