package com.smartsplit.group.service;

import com.smartsplit.common.exception.BusinessException;
import com.smartsplit.group.dto.CreateGroupRequest;
import com.smartsplit.group.entity.ExpenseGroup;
import com.smartsplit.group.entity.GroupMember;
import com.smartsplit.group.entity.GroupMemberStatus;
import com.smartsplit.group.entity.GroupRole;
import com.smartsplit.group.entity.GroupStatus;
import com.smartsplit.group.repository.ExpenseGroupRepository;
import com.smartsplit.group.repository.GroupInvitationRepository;
import com.smartsplit.group.repository.GroupMemberRepository;
import com.smartsplit.user.entity.User;
import com.smartsplit.user.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GroupServiceTest {
    private ExpenseGroupRepository groupRepository;
    private GroupMemberRepository memberRepository;
    private CurrentUserService currentUserService;
    private GroupService groupService;

    @BeforeEach
    void setUp() {
        groupRepository = mock(ExpenseGroupRepository.class);
        memberRepository = mock(GroupMemberRepository.class);
        GroupInvitationRepository invitationRepository = mock(GroupInvitationRepository.class);
        currentUserService = mock(CurrentUserService.class);
        groupService = new GroupService(
                groupRepository,
                memberRepository,
                invitationRepository,
                currentUserService
        );
    }

    @Test
    void createGroupAutomaticallyAddsCreatorAsOwner() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        when(currentUserService.getRequiredUser()).thenReturn(user);
        when(groupRepository.save(any(ExpenseGroup.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(memberRepository.findAllByGroup_IdAndStatusOrderByJoinedAtAsc(
                any(),
                eq(GroupMemberStatus.ACTIVE)
        )).thenReturn(List.of());

        groupService.create(new CreateGroupRequest(
                "Du lịch Đà Lạt",
                "Chi phí chuyến đi",
                "VND"
        ));

        ArgumentCaptor<GroupMember> memberCaptor = ArgumentCaptor.forClass(GroupMember.class);
        verify(memberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getUser()).isSameAs(user);
        assertThat(memberCaptor.getValue().getRole()).isEqualTo(GroupRole.OWNER);
        assertThat(memberCaptor.getValue().getStatus()).isEqualTo(GroupMemberStatus.ACTIVE);
    }

    @Test
    void outsiderCannotReadGroupByChangingId() {
        User outsider = mock(User.class);
        when(outsider.getId()).thenReturn(99L);
        when(currentUserService.getRequiredUser()).thenReturn(outsider);
        when(groupRepository.findByIdAndStatus(1L, GroupStatus.ACTIVE))
                .thenReturn(Optional.of(mock(ExpenseGroup.class)));
        when(memberRepository.findByGroup_IdAndUser_IdAndStatus(
                1L,
                99L,
                GroupMemberStatus.ACTIVE
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getById(1L))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("FORBIDDEN");
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                });
    }
}
