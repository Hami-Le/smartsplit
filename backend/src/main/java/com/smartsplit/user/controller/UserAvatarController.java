package com.smartsplit.user.controller;

import com.smartsplit.user.service.AvatarStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/users/avatars")
public class UserAvatarController {
    private final AvatarStorageService avatarStorageService;

    public UserAvatarController(AvatarStorageService avatarStorageService) {
        this.avatarStorageService = avatarStorageService;
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String fileName) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatarStorageService.contentType(fileName)))
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic().immutable())
                .body(avatarStorageService.load(fileName));
    }
}
