package com.smartsplit.group.repository;

import com.smartsplit.group.entity.ExpenseGroup;
import com.smartsplit.group.entity.GroupStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExpenseGroupRepository extends JpaRepository<ExpenseGroup, Long> {
    Optional<ExpenseGroup> findByIdAndStatus(Long id, GroupStatus status);
}
