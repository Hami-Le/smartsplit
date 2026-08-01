package com.smartsplit.group.controller;

import com.smartsplit.common.response.ApiResponse;
import com.smartsplit.group.dto.AcceptInvitationResponse;
import com.smartsplit.group.service.GroupService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/invitations")
public class InvitationController {
    private final GroupService groupService;

    public InvitationController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping("/{token}/accept")
    public ApiResponse<AcceptInvitationResponse> accept(@PathVariable String token) {
        return ApiResponse.ok(groupService.acceptInvitation(token));
    }
}
