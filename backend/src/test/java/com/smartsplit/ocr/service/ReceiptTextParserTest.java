package com.smartsplit.ocr.service;

import com.smartsplit.expense.entity.Category;
import com.smartsplit.expense.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReceiptTextParserTest {
    private Category food;
    private Category other;
    private ReceiptTextParser parser;

    @BeforeEach
    void setUp() {
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        food = mock(Category.class);
        other = mock(Category.class);
        when(food.getName()).thenReturn("Ăn uống");
        when(other.getName()).thenReturn("Khác");
        when(categoryRepository.findAll()).thenReturn(List.of(food, other));
        parser = new ReceiptTextParser(categoryRepository);
    }

    @Test
    void parsesVietnameseReceiptFields() {
        ParsedReceipt result = parser.parse("""
                HIGHLANDS COFFEE
                23/07/2026
                Coffee 89.000
                Cake 45.000
                VAT 13.400
                TOTAL 147.400 VND
                """);

        assertEquals("HIGHLANDS COFFEE", result.merchant());
        assertEquals(147_400L, result.totalAmount());
        assertEquals(LocalDate.of(2026, 7, 23), result.expenseDate());
        assertEquals(food, result.category());
        assertNotNull(result.confidence());
        assertTrue(result.confidence().doubleValue() >= 0.80d);
    }

    @Test
    void doesNotJoinIndependentMoneyValuesIntoOneHugeAmount() {
        ParsedReceipt result = parser.parse("""
                DanTriSoft F&B - CN1
                TỔNG TIỀN
                8.100 118.390
                Ngày 27/06/2023
                """);

        assertEquals(118_390L, result.totalAmount());
        assertTrue(result.totalAmount() < 1_000_000L);
    }

    @Test
    void parsesTimoStyleTransferAmountAndRecipient() {
        ParsedReceipt result = parser.parse("""
                TIMO
                GIAO DỊCH THÀNH CÔNG
                Số tiền giao dịch
                -150.000 VND
                Người nhận
                NGUYEN VAN AN
                Thời gian giao dịch 29/07/2026 20:15
                Mã giao dịch 202607291234567890
                """);

        assertEquals(150_000L, result.totalAmount());
        assertEquals("NGUYEN VAN AN", result.merchant());
        assertEquals(LocalDate.of(2026, 7, 29), result.expenseDate());
    }

    @Test
    void ignoresAccountAndTransactionIdentifiers() {
        ParsedReceipt result = parser.parse("""
                Người nhận: CUA HANG ABC
                Số tài khoản: 1234567890
                Mã giao dịch: 202607291234567890
                Số tiền: 325.000 VND
                """);

        assertEquals("CUA HANG ABC", result.merchant());
        assertEquals(325_000L, result.totalAmount());
    }
}
