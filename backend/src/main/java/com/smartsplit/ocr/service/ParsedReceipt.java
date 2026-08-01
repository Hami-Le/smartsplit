package com.smartsplit.ocr.service;

import com.smartsplit.expense.entity.Category;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParsedReceipt(
        String merchant,
        Long totalAmount,
        LocalDate expenseDate,
        Category category,
        BigDecimal confidence
) {
}
