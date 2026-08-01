package com.smartsplit.user.controller;

import com.smartsplit.common.response.ApiResponse;
import com.smartsplit.user.dto.ChangePasswordRequest;
import com.smartsplit.user.dto.UpdateProfileRequest;
import com.smartsplit.user.dto.UserProfileResponse;
import com.smartsplit.user.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me")
public class UserAccountController {
    private final UserAccountService userAccountService;

    public UserAccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping
    public ApiResponse<UserProfileResponse> getProfile() {
        return ApiResponse.ok(userAccountService.getProfile());
    }

    @PatchMapping
    public ApiResponse<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userAccountService.updateProfile(request));
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userAccountService.changePassword(request);
    }
}
