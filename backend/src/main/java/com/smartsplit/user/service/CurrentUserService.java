package com.smartsplit.user.service;

import com.smartsplit.common.exception.BusinessException;
import com.smartsplit.user.entity.User;
import com.smartsplit.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getRequiredUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(
                    "AUTHENTICATION_REQUIRED",
                    "Bạn cần đăng nhập để thực hiện thao tác này",
                    HttpStatus.UNAUTHORIZED
            );
        }

        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new BusinessException(
                        "CURRENT_USER_NOT_FOUND",
                        "Không tìm thấy tài khoản đang đăng nhập",
                        HttpStatus.UNAUTHORIZED
                ));
    }
}
