package com.smartsplit.personal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpsertPersonalExpenseRequest(
        @NotBlank(message = "Tên khoản chi không được để trống")
        @Size(max = 180, message = "Tên khoản chi tối đa 180 ký tự")
        String title,

        @NotNull(message = "Số tiền không được để trống")
        @Positive(message = "Số tiền phải lớn hơn 0")
        Long amount,

        @NotNull(message = "Ngày chi không được để trống")
        LocalDate expenseDate,

        Long categoryId,

        @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
        String note
) {}
