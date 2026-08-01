package com.smartsplit.group.controller;

import com.smartsplit.common.response.ApiResponse;
import com.smartsplit.group.dto.*;
import com.smartsplit.group.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/groups")
public class GroupController {
    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GroupDetailResponse> create(@Valid @RequestBody CreateGroupRequest request) {
        return ApiResponse.ok(groupService.create(request));
    }

    @GetMapping
    public ApiResponse<List<GroupSummaryResponse>> listMine() {
        return ApiResponse.ok(groupService.listMine());
    }

    @GetMapping("/{groupId}")
    public ApiResponse<GroupDetailResponse> getById(@PathVariable Long groupId) {
        return ApiResponse.ok(groupService.getById(groupId));
    }

    @PatchMapping("/{groupId}")
    public ApiResponse<GroupDetailResponse> update(
            @PathVariable Long groupId,
            @Valid @RequestBody UpdateGroupRequest request
    ) {
        return ApiResponse.ok(groupService.update(groupId, request));
    }

    @DeleteMapping("/{groupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable Long groupId) {
        groupService.archive(groupId);
    }

    @PostMapping("/{groupId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<InvitationResponse> invite(
            @PathVariable Long groupId,
            @Valid @RequestBody InviteMemberRequest request
    ) {
        return ApiResponse.ok(groupService.invite(groupId, request));
    }

    @PatchMapping("/{groupId}/members/{userId}/role")
    public ApiResponse<GroupMemberResponse> updateMemberRole(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateMemberRoleRequest request
    ) {
        return ApiResponse.ok(groupService.updateMemberRole(groupId, userId, request));
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long groupId, @PathVariable Long userId) {
        groupService.removeMember(groupId, userId);
    }
}
