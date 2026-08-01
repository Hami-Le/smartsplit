package com.smartsplit.balance.service;

import com.smartsplit.balance.dto.BalanceEntry;
import com.smartsplit.balance.dto.TransferSuggestion;
import com.smartsplit.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

@Service
public class DebtSimplificationService {

    public List<TransferSuggestion> simplify(List<BalanceEntry> balances) {
        long total = balances.stream().mapToLong(BalanceEntry::balance).sum();
        if (total != 0) {
            throw new BusinessException(
                    "UNBALANCED_LEDGER",
                    "Tổng số dư phải bằng 0 nhưng hiện tại là " + total
            );
        }

        PriorityQueue<Node> debtors = new PriorityQueue<>(
                Comparator.comparingLong(Node::amount).reversed()
        );
        PriorityQueue<Node> creditors = new PriorityQueue<>(
                Comparator.comparingLong(Node::amount).reversed()
        );

        for (BalanceEntry entry : balances) {
            if (entry.balance() < 0) {
                debtors.add(new Node(entry.memberId(), entry.memberName(), -entry.balance()));
            } else if (entry.balance() > 0) {
                creditors.add(new Node(entry.memberId(), entry.memberName(), entry.balance()));
            }
        }

        List<TransferSuggestion> result = new ArrayList<>();
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            Node debtor = debtors.poll();
            Node creditor = creditors.poll();
            long transferAmount = Math.min(debtor.amount(), creditor.amount());

            result.add(new TransferSuggestion(
                    debtor.memberId(),
                    debtor.memberName(),
                    creditor.memberId(),
                    creditor.memberName(),
                    transferAmount
            ));

            long debtorRemaining = debtor.amount() - transferAmount;
            long creditorRemaining = creditor.amount() - transferAmount;
            if (debtorRemaining > 0) {
                debtors.add(debtor.withAmount(debtorRemaining));
            }
            if (creditorRemaining > 0) {
                creditors.add(creditor.withAmount(creditorRemaining));
            }
        }

        return List.copyOf(result);
    }

    private record Node(Long memberId, String memberName, long amount) {
        Node withAmount(long newAmount) {
            return new Node(memberId, memberName, newAmount);
        }
    }
}
