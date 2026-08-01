package com.smartsplit.user.service;

import com.smartsplit.common.exception.BusinessException;
import com.smartsplit.user.dto.ChangePasswordRequest;
import com.smartsplit.user.dto.UpdateProfileRequest;
import com.smartsplit.user.dto.UserProfileResponse;
import com.smartsplit.user.entity.User;
import com.smartsplit.user.repository.UserRepository;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AvatarStorageService avatarStorageService;

    public UserAccountService(
            CurrentUserService currentUserService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AvatarStorageService avatarStorageService
    ) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.avatarStorageService = avatarStorageService;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile() {
        return toResponse(currentUserService.getRequiredUser());
    }

    @Transactional
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        User user = currentUserService.getRequiredUser();
        user.setFullName(request.fullName().trim());
        user.setPhone(normalizePhone(request.phone()));
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = currentUserService.getRequiredUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("CURRENT_PASSWORD_INVALID", "Mật khẩu hiện tại không đúng");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public UserProfileResponse updateAvatar(MultipartFile file) {
        User user = currentUserService.getRequiredUser();
        String oldAvatarUrl = user.getAvatarUrl();
        String newAvatarUrl = avatarStorageService.store(file);
        try {
            user.setAvatarUrl(newAvatarUrl);
            User saved = userRepository.save(user);
            avatarStorageService.delete(oldAvatarUrl);
            return toResponse(saved);
        } catch (RuntimeException exception) {
            avatarStorageService.delete(newAvatarUrl);
            throw exception;
        }
    }

    @Transactional
    public UserProfileResponse removeAvatar() {
        User user = currentUserService.getRequiredUser();
        String oldAvatarUrl = user.getAvatarUrl();
        user.setAvatarUrl(null);
        User saved = userRepository.save(user);
        avatarStorageService.delete(oldAvatarUrl);
        return toResponse(saved);
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        return phone.trim();
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getRole().name()
        );
    }
}
