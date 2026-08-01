package com.smartsplit.group.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateGroupRequest(
        @Size(min = 1, max = 150, message = "Tên nhóm phải có từ 1 đến 150 ký tự")
        String name,

        @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
        String description,

        @Pattern(regexp = "^[A-Z]{3}$", message = "Mã tiền tệ phải gồm 3 chữ cái viết hoa")
        String defaultCurrency
) {}
