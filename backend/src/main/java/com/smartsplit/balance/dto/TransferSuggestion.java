package com.smartsplit.balance.dto;

public record TransferSuggestion(
        Long fromMemberId,
        String fromMemberName,
        Long toMemberId,
        String toMemberName,
        long amount
) {}
