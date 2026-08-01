package com.smartsplit.group.repository;

import com.smartsplit.group.entity.GroupInvitation;
import com.smartsplit.group.entity.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, Long> {
    Optional<GroupInvitation> findByTokenHash(String tokenHash);

    Optional<GroupInvitation> findFirstByGroup_IdAndEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(
            Long groupId,
            String email,
            InvitationStatus status
    );
}
