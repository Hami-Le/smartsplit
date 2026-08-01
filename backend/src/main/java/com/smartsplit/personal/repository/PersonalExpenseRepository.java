package com.smartsplit.personal.repository;

import com.smartsplit.personal.entity.PersonalExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PersonalExpenseRepository extends JpaRepository<PersonalExpense, Long> {
    List<PersonalExpense> findAllByUser_IdAndExpenseDateBetweenOrderByExpenseDateDescCreatedAtDesc(
            Long userId,
            LocalDate from,
            LocalDate to
    );

    Optional<PersonalExpense> findByIdAndUser_Id(Long id, Long userId);
}
