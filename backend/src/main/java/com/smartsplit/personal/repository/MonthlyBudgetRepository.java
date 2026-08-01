package com.smartsplit.personal.repository;

import com.smartsplit.personal.entity.MonthlyBudget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface MonthlyBudgetRepository extends JpaRepository<MonthlyBudget, Long> {
    Optional<MonthlyBudget> findByUser_IdAndBudgetMonth(Long userId, LocalDate budgetMonth);
}
