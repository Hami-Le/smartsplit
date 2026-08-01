package com.smartsplit.group.repository;

import com.smartsplit.group.entity.GroupMember;
import com.smartsplit.group.entity.GroupMemberStatus;
import com.smartsplit.group.entity.GroupStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    Optional<GroupMember> findByGroup_IdAndUser_Id(Long groupId, Long userId);

    Optional<GroupMember> findByGroup_IdAndUser_IdAndStatus(
            Long groupId,
            Long userId,
            GroupMemberStatus status
    );

    List<GroupMember> findAllByUser_IdAndStatusAndGroup_StatusOrderByJoinedAtDesc(
            Long userId,
            GroupMemberStatus memberStatus,
            GroupStatus groupStatus
    );

    List<GroupMember> findAllByGroup_IdAndStatusOrderByJoinedAtAsc(
            Long groupId,
            GroupMemberStatus status
    );

    List<GroupMember> findAllByGroup_IdOrderByJoinedAtAsc(Long groupId);

    long countByGroup_IdAndStatus(Long groupId, GroupMemberStatus status);
}
