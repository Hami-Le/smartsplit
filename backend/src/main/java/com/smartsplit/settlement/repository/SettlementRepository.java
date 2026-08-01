package com.smartsplit.settlement.repository;

import com.smartsplit.settlement.entity.Settlement;
import com.smartsplit.settlement.entity.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    List<Settlement> findAllByGroup_IdOrderBySettledAtDescCreatedAtDesc(Long groupId);

    List<Settlement> findAllByGroup_IdAndStatusOrderBySettledAtDescCreatedAtDesc(
            Long groupId,
            SettlementStatus status
    );
}
