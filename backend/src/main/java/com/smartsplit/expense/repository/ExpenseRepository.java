package com.smartsplit.expense.repository;

import com.smartsplit.expense.entity.Expense;
import com.smartsplit.expense.entity.ExpenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    Optional<Expense> findByIdAndStatus(Long id, ExpenseStatus status);

    List<Expense> findAllByGroup_IdAndStatusOrderByExpenseDateDescCreatedAtDesc(
            Long groupId,
            ExpenseStatus status
    );
}
