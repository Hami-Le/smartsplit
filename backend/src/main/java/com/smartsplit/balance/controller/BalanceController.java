package com.smartsplit.balance.controller;

import com.smartsplit.balance.dto.GroupBalanceResponse;
import com.smartsplit.balance.dto.SimplifyDebtRequest;
import com.smartsplit.balance.dto.TransferSuggestion;
import com.smartsplit.balance.service.DebtSimplificationService;
import com.smartsplit.balance.service.GroupBalanceService;
import com.smartsplit.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BalanceController {
    private final DebtSimplificationService debtSimplificationService;
    private final GroupBalanceService groupBalanceService;

    public BalanceController(
            DebtSimplificationService debtSimplificationService,
            GroupBalanceService groupBalanceService
    ) {
        this.debtSimplificationService = debtSimplificationService;
        this.groupBalanceService = groupBalanceService;
    }

    @PostMapping("/balances/simplify")
    public ApiResponse<List<TransferSuggestion>> simplify(
            @Valid @RequestBody SimplifyDebtRequest request
    ) {
        return ApiResponse.ok(debtSimplificationService.simplify(request.balances()));
    }

    @GetMapping("/groups/{groupId}/balances")
    public ApiResponse<GroupBalanceResponse> balances(@PathVariable Long groupId) {
        return ApiResponse.ok(groupBalanceService.getBalances(groupId));
    }

    @GetMapping("/groups/{groupId}/suggested-transfers")
    public ApiResponse<List<TransferSuggestion>> suggestions(@PathVariable Long groupId) {
        return ApiResponse.ok(groupBalanceService.getBalances(groupId).suggestedTransfers());
    }
}
