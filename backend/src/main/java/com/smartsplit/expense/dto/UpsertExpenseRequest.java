package com.smartsplit.expense.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public record UpsertExpenseRequest(
        @NotBlank(message = "Tên khoản chi không được để trống")
        @Size(max = 180, message = "Tên khoản chi tối đa 180 ký tự")
        String title,

        @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
        String description,

        @NotNull(message = "Tổng tiền không được để trống")
        @Positive(message = "Tổng tiền phải lớn hơn 0")
        Long totalAmount,

        @NotNull(message = "Ngày chi không được để trống")
        LocalDate expenseDate,

        Long categoryId,

        @NotEmpty(message = "Phải có ít nhất một người trả")
        List<@Valid ExpensePayerInput> payers,

        @NotNull(message = "Thông tin chia tiền không được để trống")
        @Valid
        ExpenseSplitInput split
) {}
