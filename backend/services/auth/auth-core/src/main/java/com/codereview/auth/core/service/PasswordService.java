package com.codereview.auth.core.service;

import com.codereview.auth.core.domain.PasswordResetToken;
import com.codereview.auth.core.domain.User;
import com.codereview.auth.core.repository.PasswordResetTokenRepository;
import com.codereview.auth.core.repository.UserRepository;
import com.think.platform.shared.common.exception.BusinessException;
import com.think.platform.shared.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Password Service - Auth Service
 *
 * 负责密码相关业务逻辑
 *
 * @author Auth Service Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordService {

    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;

    @Value("${auth.password.reset-token-expiry-hours:24}")
    private int resetTokenExpiryHours;

    /**
     * 编码密码（加密）
     */
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 验证密码
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        // 验证旧密码
        if (!matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException("旧密码不正确");
        }

        // 设置新密码
        user.setPasswordHash(encodePassword(newPassword));
        userRepository.save(user);

        log.info("Password changed for user: {}", user.getUsername());
    }

    /**
     * 生成密码重置令牌
     */
    @Transactional
    public String generateResetToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        // 使该用户之前的令牌失效
        passwordResetTokenRepository.invalidateAllUserTokens(user.getId());

        // 创建新令牌
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(resetTokenExpiryHours))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);
        log.info("Password reset token generated for user: {}", user.getUsername());

        return token;
    }

    /**
     * 验证重置令牌
     */
    public PasswordResetToken validateResetToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("重置令牌不存在"));

        if (!resetToken.isValid()) {
            throw new BusinessException("重置令牌已失效或已过期");
        }

        return resetToken;
    }

    /**
     * 重置密码
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = validateResetToken(token);

        User user = resetToken.getUser();
        user.setPasswordHash(encodePassword(newPassword));
        userRepository.save(user);

        // 标记令牌为已使用
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset successfully for user: {}", user.getUsername());
    }

    /**
     * 清理过期的令牌
     */
    @Transactional
    public void cleanupExpiredTokens() {
        passwordResetTokenRepository.deleteExpiredOrUsedTokens(LocalDateTime.now());
        log.info("Expired password reset tokens cleaned up");
    }
}
