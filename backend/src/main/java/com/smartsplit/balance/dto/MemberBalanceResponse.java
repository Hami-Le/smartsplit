package com.smartsplit.balance.dto;

public record MemberBalanceResponse(
        Long userId,
        String fullName,
        String email,
        String avatarUrl,
        String membershipStatus,
        long paidAmount,
        long shareAmount,
        long sentAmount,
        long receivedAmount,
        long balance
) {}
