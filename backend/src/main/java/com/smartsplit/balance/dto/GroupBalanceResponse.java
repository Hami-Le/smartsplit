package com.smartsplit.balance.dto;

import java.util.List;

public record GroupBalanceResponse(
        Long groupId,
        String groupName,
        String currency,
        String currentUserRole,
        long totalExpense,
        long totalSettled,
        List<MemberBalanceResponse> members,
        List<TransferSuggestion> suggestedTransfers
) {}
