package com.smartsplit.user.controller;

import com.smartsplit.common.response.ApiResponse;
import com.smartsplit.user.dto.ChangePasswordRequest;
import com.smartsplit.user.dto.UpdateProfileRequest;
import com.smartsplit.user.dto.UserProfileResponse;
import com.smartsplit.user.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserProfileResponse> updateAvatar(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(userAccountService.updateAvatar(file));
    }

    @DeleteMapping("/avatar")
    public ApiResponse<UserProfileResponse> removeAvatar() {
        return ApiResponse.ok(userAccountService.removeAvatar());
    }
}
