package com.smartsplit.report.dto;

import java.math.BigDecimal;

public record MemberSpendingResponse(
        Long userId,
        String fullName,
        String email,
        String avatarUrl,
        String membershipStatus,
        long paidAmount,
        long shareAmount,
        BigDecimal sharePercentage
) {}
