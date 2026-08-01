package com.smartsplit.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 120, message = "Họ tên không được vượt quá 120 ký tự")
        String fullName,

        @Size(max = 30, message = "Số điện thoại không được vượt quá 30 ký tự")
        @Pattern(
                regexp = "^$|^[0-9+(). -]{7,30}$",
                message = "Số điện thoại không đúng định dạng"
        )
        String phone
) {}
