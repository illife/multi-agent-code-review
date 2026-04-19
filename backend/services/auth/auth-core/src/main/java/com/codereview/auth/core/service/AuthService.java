package com.codereview.auth.core.service;

import com.codereview.auth.core.domain.User;
import com.codereview.auth.core.repository.UserRepository;
import com.think.platform.shared.common.dto.LoginRequest;
import com.think.platform.shared.common.dto.RegisterRequest;
import com.think.platform.shared.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Auth Service - Auth Service
 *
 * 负责认证核心业务逻辑
 *
 * @author Auth Service Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final PasswordService passwordService;
    private final TokenService tokenService;
    private final UserRepository userRepository;

    /**
     * 用户登录
     */
    @Transactional
    public Map<String, String> login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());

        try {
            // 使用 Spring Security 进行认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // 获取用户信息
            User user = userService.getUserByUsername(request.getUsername());

            // 检查用户是否激活
            if (!user.getIsActive()) {
                throw new BusinessException("用户账户已被禁用");
            }

            // 更新最后登录时间
            userService.updateLastLoginTime(user.getId());

            // 生成访问令牌和刷新令牌
            String accessToken = tokenService.generateAccessToken(user);
            String refreshToken = tokenService.generateRefreshToken(user);

            log.info("User logged in successfully: {}", request.getUsername());

            // 返回令牌信息
            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", accessToken);
            tokens.put("refreshToken", refreshToken);
            tokens.put("tokenType", "Bearer");
            tokens.put("userId", user.getId().toString());
            tokens.put("username", user.getUsername());

            return tokens;

        } catch (org.springframework.security.core.AuthenticationException e) {
            log.error("Authentication failed for user: {}", request.getUsername());
            throw new BusinessException("用户名或密码错误");
        }
    }

    /**
     * 用户注册
     */
    @Transactional
    public Map<String, String> register(RegisterRequest request) {
        log.info("Registration attempt for user: {}", request.getUsername());

        // 注册用户
        User user = userService.registerUser(request);

        // 生成令牌
        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);

        log.info("User registered successfully: {}", request.getUsername());

        // 返回令牌信息
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        tokens.put("tokenType", "Bearer");
        tokens.put("userId", user.getId().toString());
        tokens.put("username", user.getUsername());

        return tokens;
    }

    /**
     * 刷新访问令牌
     */
    @Transactional
    public Map<String, String> refreshToken(String refreshTokenStr) {
        log.info("Token refresh request");

        // 验证刷新令牌
        String newAccessToken = tokenService.refreshAccessToken(refreshTokenStr);

        // 返回新的访问令牌
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
        tokens.put("tokenType", "Bearer");

        return tokens;
    }

    /**
     * 登出（撤销刷新令牌）
     */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isEmpty()) {
            tokenService.revokeRefreshToken(refreshToken);
            log.info("User logged out");
        }
    }

    /**
     * 登出所有设备
     */
    @Transactional
    public void logoutAll(Long userId) {
        tokenService.revokeAllUserTokens(userId);
        log.info("User logged out from all devices, userId: {}", userId);
    }

    /**
     * 验证令牌
     */
    public boolean validateToken(String token) {
        return tokenService.validateRefreshToken(token) != null;
    }

    /**
     * 获取当前登录用户
     */
    public User getCurrentUser(Long userId) {
        return userService.getUserById(userId);
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        passwordService.changePassword(userId, oldPassword, newPassword);
        log.info("Password changed for userId: {}", userId);
    }

    /**
     * 忘记密码 - 发送重置令牌
     */
    @Transactional
    public String forgotPassword(String email) {
        String resetToken = passwordService.generateResetToken(email);
        log.info("Password reset token generated for email: {}", email);

        // TODO: 发送邮件
        // emailService.sendPasswordResetEmail(email, resetToken);

        return resetToken;
    }

    /**
     * 重置密码
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        passwordService.resetPassword(token, newPassword);
        log.info("Password reset successfully");
    }
}
