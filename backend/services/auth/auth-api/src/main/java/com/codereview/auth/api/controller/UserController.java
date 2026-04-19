package com.codereview.auth.api.controller;

import com.codereview.auth.api.dto.UpdateUserRequest;
import com.codereview.auth.api.dto.UserInfo;
import com.codereview.auth.core.domain.User;
import com.codereview.auth.core.service.UserService;
import com.think.platform.shared.common.dto.UserDTO;
import com.think.platform.shared.common.result.Result;
import com.think.platform.shared.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * User Controller - Auth Service
 *
 * User management REST API (Admin)
 *
 * @author Auth Service Team
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Get users with pagination (Admin)
     * GET /api/users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Page<UserInfo>> getUsers(Pageable pageable) {
        Page<User> users = userService.getUsers(pageable);

        List<UserInfo> userInfos = users.getContent().stream()
                .map(this::mapToUserInfo)
                .collect(Collectors.toList());

        Page<UserInfo> result = new PageImpl<>(userInfos, pageable, users.getTotalElements());
        return Result.success(result);
    }

    /**
     * Get user by ID
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserDTO> getUser(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return Result.success(mapToUserDTO(user));
    }

    /**
     * Update user (Admin)
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        User user = userService.updateUser(id, request.getFullName());
        return Result.success(mapToUserDTO(user));
    }

    /**
     * Delete user (Admin)
     * DELETE /api/users/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    /**
     * Enable user
     * PUT /api/users/{id}/enable
     */
    @PutMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> enableUser(@PathVariable Long id) {
        userService.enableUser(id);
        return Result.success();
    }

    /**
     * Disable user
     * PUT /api/users/{id}/disable
     */
    @PutMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> disableUser(@PathVariable Long id) {
        userService.disableUser(id);
        return Result.success();
    }

    /**
     * Assign role (Admin)
     * PUT /api/users/{id}/role
     */
    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> assignRole(
            @PathVariable Long id,
            @RequestBody RoleRequest request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        if (id.equals(currentUserId)) {
            return Result.error("Cannot modify your own role");
        }
        userService.updateUserRole(id, User.Role.valueOf(request.getRole()));
        return Result.success();
    }

    private UserDTO mapToUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .build();
    }

    private UserInfo mapToUserInfo(User user) {
        return UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .department(null) // Removed from User entity
                .isActive(user.getIsActive())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .roles(user.getRole() != null ? java.util.Set.of(user.getRole().name()) : java.util.Set.of())
                .build();
    }

    /**
     * Role request DTO
     */
    public static class RoleRequest {
        private String role;

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}
