package com.codereview.auth.api.controller;

import com.codereview.auth.api.dto.RefreshTokenRequest;
import com.codereview.auth.api.dto.UpdateProfileRequest;
import com.codereview.auth.api.dto.UserInfo;
import com.codereview.auth.core.service.AuthService;
import com.codereview.auth.core.service.UserService;
import com.think.platform.shared.common.dto.AuthResponse;
import com.think.platform.shared.common.dto.ChangePasswordRequest;
import com.think.platform.shared.common.dto.LoginRequest;
import com.think.platform.shared.common.dto.PasswordResetConfirmRequest;
import com.think.platform.shared.common.dto.PasswordResetRequest;
import com.think.platform.shared.common.dto.RegisterRequest;
import com.think.platform.shared.common.dto.UserDTO;
import com.think.platform.shared.common.result.Result;
import com.think.platform.shared.security.JwtTokenProvider;
import com.think.platform.shared.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Auth Controller - Auth Service
 *
 * 认证服务REST API
 *
 * @author Auth Service Team
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 用户注册
     * POST /api/auth/register
     */
    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request: username={}", request.getUsername());
        Map<String, String> tokens = authService.register(request);

        AuthResponse response = AuthResponse.of(
                tokens.get("accessToken"),
                tokens.get("refreshToken"),
                Long.parseLong(tokens.get("userId")),
                tokens.get("username"),
                "USER"
        );

        return Result.success(response);
    }

    /**
     * 用户登录
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request: username={}", request.getUsername());
        Map<String, String> tokens = authService.login(request);

        AuthResponse response = AuthResponse.of(
                tokens.get("accessToken"),
                tokens.get("refreshToken"),
                Long.parseLong(tokens.get("userId")),
                tokens.get("username"),
                "USER"
        );

        return Result.success(response);
    }

    /**
     * 刷新Token
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public Result<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request");
        Map<String, String> tokens = authService.refreshToken(request.getRefreshToken());

        AuthResponse response = new AuthResponse();
        response.setAccessToken(tokens.get("accessToken"));
        response.setTokenType(tokens.get("tokenType"));

        return Result.success(response);
    }

    /**
     * 登出
     * POST /api/auth/logout
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String refreshToken = ""; // TODO: 从请求中获取 refresh token
        authService.logout(refreshToken);
        return Result.success();
    }

    /**
     * 获取当前用户信息
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public Result<UserDTO> getCurrentUser(HttpServletRequest request) {
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new com.think.platform.shared.common.exception.AuthenticationException("未登录"));
        var user = userService.getUserById(userId);

        UserDTO userDTO = UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole() != null ? user.getRole().name() : "USER")
                .isActive(user.getIsActive())
                .build();

        return Result.success(userDTO);
    }

    /**
     * 更新个人资料
     * PUT /api/auth/profile
     */
    @PutMapping("/profile")
    public Result<UserDTO> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new com.think.platform.shared.common.exception.AuthenticationException("未登录"));
        var user = userService.updateUser(userId, request.getFullName());

        UserDTO userDTO = UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole() != null ? user.getRole().name() : "USER")
                .isActive(user.getIsActive())
                .build();

        return Result.success(userDTO);
    }

    /**
     * 修改密码
     * POST /api/auth/change-password
     */
    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new com.think.platform.shared.common.exception.AuthenticationException("未登录"));
        authService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
        return Result.success();
    }

    /**
     * 忘记密码
     * POST /api/auth/forgot-password
     */
    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.forgotPassword(request.getEmail());
        return Result.success();
    }

    /**
     * 重置密码
     * POST /api/auth/reset-password
     */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return Result.success();
    }

    /**
     * 验证Token
     * GET /api/auth/validate
     */
    @GetMapping("/validate")
    public Result<Boolean> validateToken(HttpServletRequest request) {
        String token = jwtTokenProvider.extractTokenFromHeader(request.getHeader("Authorization"));
        boolean valid = jwtTokenProvider.validateToken(token);
        return Result.success(valid);
    }

    /**
     * 健康检查
     * GET /api/auth/health
     */
    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        Map<String, String> health = Map.of(
                "status", "UP",
                "service", "auth-service"
        );
        return Result.success(health);
    }
}
