package com.smartsplit.expense.controller;

import com.smartsplit.common.response.ApiResponse;
import com.smartsplit.expense.dto.ExpenseResponse;
import com.smartsplit.expense.dto.UpsertExpenseRequest;
import com.smartsplit.expense.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
public class ExpenseController {
    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping("/groups/{groupId}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExpenseResponse> create(
            @PathVariable Long groupId,
            @Valid @RequestBody UpsertExpenseRequest request
    ) {
        return ApiResponse.ok(expenseService.create(groupId, request));
    }

    @GetMapping("/groups/{groupId}/expenses")
    public ApiResponse<List<ExpenseResponse>> list(
            @PathVariable Long groupId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search
    ) {
        return ApiResponse.ok(expenseService.list(groupId, from, to, categoryId, search));
    }

    @GetMapping("/expenses/{expenseId}")
    public ApiResponse<ExpenseResponse> getById(@PathVariable Long expenseId) {
        return ApiResponse.ok(expenseService.getById(expenseId));
    }

    @PutMapping("/expenses/{expenseId}")
    public ApiResponse<ExpenseResponse> update(
            @PathVariable Long expenseId,
            @Valid @RequestBody UpsertExpenseRequest request
    ) {
        return ApiResponse.ok(expenseService.update(expenseId, request));
    }

    @DeleteMapping("/expenses/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long expenseId) {
        expenseService.delete(expenseId);
    }
}
