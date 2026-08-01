package com.smartsplit.personal.controller;

import com.smartsplit.common.response.ApiResponse;
import com.smartsplit.personal.dto.MonthlyBudgetRequest;
import com.smartsplit.personal.dto.PersonalExpenseResponse;
import com.smartsplit.personal.dto.PersonalFinanceSummaryResponse;
import com.smartsplit.personal.dto.UpsertPersonalExpenseRequest;
import com.smartsplit.personal.service.PersonalFinanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/personal-finance")
public class PersonalFinanceController {
    private final PersonalFinanceService personalFinanceService;

    public PersonalFinanceController(PersonalFinanceService personalFinanceService) {
        this.personalFinanceService = personalFinanceService;
    }

    @GetMapping
    public ApiResponse<PersonalFinanceSummaryResponse> getSummary(
            @RequestParam(required = false) String month
    ) {
        return ApiResponse.ok(personalFinanceService.getSummary(month));
    }

    @PostMapping("/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PersonalExpenseResponse> createExpense(
            @Valid @RequestBody UpsertPersonalExpenseRequest request
    ) {
        return ApiResponse.ok(personalFinanceService.create(request));
    }

    @PutMapping("/expenses/{expenseId}")
    public ApiResponse<PersonalExpenseResponse> updateExpense(
            @PathVariable Long expenseId,
            @Valid @RequestBody UpsertPersonalExpenseRequest request
    ) {
        return ApiResponse.ok(personalFinanceService.update(expenseId, request));
    }

    @DeleteMapping("/expenses/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable Long expenseId) {
        personalFinanceService.delete(expenseId);
    }

    @PutMapping("/budgets/{month}")
    public ApiResponse<PersonalFinanceSummaryResponse> setBudget(
            @PathVariable String month,
            @Valid @RequestBody MonthlyBudgetRequest request
    ) {
        return ApiResponse.ok(personalFinanceService.setBudget(month, request));
    }

    @DeleteMapping("/budgets/{month}")
    public ApiResponse<PersonalFinanceSummaryResponse> deleteBudget(@PathVariable String month) {
        return ApiResponse.ok(personalFinanceService.deleteBudget(month));
    }
}
