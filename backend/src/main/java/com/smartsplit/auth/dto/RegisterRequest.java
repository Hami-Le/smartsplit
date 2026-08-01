package com.smartsplit.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 120, message = "Họ tên không được vượt quá 120 ký tự")
        String fullName,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, max = 72, message = "Mật khẩu phải có từ 8 đến 72 ký tự")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "Mật khẩu phải có ít nhất một chữ và một số"
        )
        String password
) {}
