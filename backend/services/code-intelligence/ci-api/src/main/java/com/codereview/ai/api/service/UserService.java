package com.codereview.ai.api.service;

import com.codereview.ai.api.dto.*;
import com.codereview.ai.domain.model.Role;
import com.codereview.ai.domain.model.User;
import com.codereview.ai.domain.repository.RoleRepository;
import com.codereview.ai.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

/**
 * User Service for Code Review AI
 *
 * Handles user management operations
 *
 * @author Code Review AI Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Find user by username with roles
     */
    public Optional<User> findByUsernameWithRoles(String username) {
        return userRepository.findByUsernameWithRoles(username);
    }

    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Find user by ID
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Check if username exists
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Check if email exists
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Register new user
     */
    @Transactional
    public User register(RegisterRequest request) {
        log.info("Registering new user: username={}, email={}", request.getUsername(), request.getEmail());

        // Check if username already exists
        if (existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Check if email already exists
        if (existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .department(request.getDepartment())
                .isActive(true)
                .roles(new HashSet<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Assign default USER role
        roleRepository.findByName("USER").ifPresent(user.getRoles()::add);

        user = userRepository.save(user);
        log.info("User registered successfully: userId={}", user.getId());
        return user;
    }

    /**
     * Update user profile
     */
    @Transactional
    public User updateProfile(Long userId, UpdateProfileRequest request) {
        log.info("Updating user profile: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getDepartment() != null) {
            user.setDepartment(request.getDepartment());
        }

        user = userRepository.save(user);
        log.info("User profile updated: userId={}", userId);
        return user;
    }

    /**
     * Update last login time
     */
    @Transactional
    public void updateLastLoginTime(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    /**
     * Verify user password
     */
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * Change password
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        log.info("Changing password: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify current password
        if (!verifyPassword(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password changed successfully: userId={}", userId);
    }

    /**
     * Deactivate user
     */
    @Transactional
    public void deactivateUser(Long userId) {
        log.info("Deactivating user: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setIsActive(false);
        userRepository.save(user);

        log.info("User deactivated: userId={}", userId);
    }

    /**
     * Activate user
     */
    @Transactional
    public void activateUser(Long userId) {
        log.info("Activating user: userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setIsActive(true);
        userRepository.save(user);

        log.info("User activated: userId={}", userId);
    }

    /**
     * Convert User to UserInfo DTO
     */
    public UserInfo toUserInfo(User user) {
        return UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .department(user.getDepartment())
                .isActive(user.getIsActive())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .toList())
                .build();
    }
}
