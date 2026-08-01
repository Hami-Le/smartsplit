package com.smartsplit.settlement.controller;

import com.smartsplit.common.response.ApiResponse;
import com.smartsplit.settlement.dto.CreateSettlementRequest;
import com.smartsplit.settlement.dto.SettlementResponse;
import com.smartsplit.settlement.service.SettlementService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SettlementController {
    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping("/groups/{groupId}/settlements")
    public ApiResponse<List<SettlementResponse>> list(@PathVariable Long groupId) {
        return ApiResponse.ok(settlementService.list(groupId));
    }

    @PostMapping("/groups/{groupId}/settlements")
    public ApiResponse<SettlementResponse> create(
            @PathVariable Long groupId,
            @Valid @RequestBody CreateSettlementRequest request
    ) {
        return ApiResponse.ok(settlementService.create(groupId, request));
    }

    @DeleteMapping("/settlements/{settlementId}")
    public ApiResponse<Void> cancel(@PathVariable Long settlementId) {
        settlementService.cancel(settlementId);
        return ApiResponse.ok(null);
    }
}
