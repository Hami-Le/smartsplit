package com.smartsplit.report.dto;

import com.smartsplit.balance.dto.MemberBalanceResponse;

import java.util.List;

public record ReportSnapshot(
        DashboardResponse dashboard,
        List<ReportExpenseRow> expenses,
        List<MemberBalanceResponse> balances,
        List<ReportSettlementRow> settlements
) {}
