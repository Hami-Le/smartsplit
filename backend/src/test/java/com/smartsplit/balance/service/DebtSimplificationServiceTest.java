package com.smartsplit.balance.service;

import com.smartsplit.balance.dto.BalanceEntry;
import com.smartsplit.balance.dto.TransferSuggestion;
import com.smartsplit.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DebtSimplificationServiceTest {
    private final DebtSimplificationService service = new DebtSimplificationService();

    @Test
    void shouldSimplifyBalancedLedger() {
        List<BalanceEntry> balances = List.of(
                new BalanceEntry(1L, "A", -100_000),
                new BalanceEntry(2L, "B", -200_000),
                new BalanceEntry(3L, "C", 150_000),
                new BalanceEntry(4L, "D", 150_000)
        );

        List<TransferSuggestion> transfers = service.simplify(balances);

        assertEquals(3, transfers.size());
        assertEquals(300_000, transfers.stream().mapToLong(TransferSuggestion::amount).sum());
        assertTrue(transfers.stream().allMatch(transfer -> transfer.amount() > 0));
    }

    @Test
    void shouldReturnEmptyWhenEveryoneIsSettled() {
        List<BalanceEntry> balances = List.of(
                new BalanceEntry(1L, "A", 0),
                new BalanceEntry(2L, "B", 0)
        );

        assertTrue(service.simplify(balances).isEmpty());
    }

    @Test
    void shouldRejectLedgerWhoseSumIsNotZero() {
        List<BalanceEntry> balances = List.of(
                new BalanceEntry(1L, "A", -100_000),
                new BalanceEntry(2L, "B", 90_000)
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.simplify(balances)
        );
        assertEquals("UNBALANCED_LEDGER", exception.getCode());
    }
}
