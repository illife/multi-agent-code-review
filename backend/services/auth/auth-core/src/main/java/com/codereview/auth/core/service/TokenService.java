package com.codereview.auth.core.service;

import com.codereview.auth.core.domain.RefreshToken;
import com.codereview.auth.core.domain.User;
import com.codereview.auth.core.repository.RefreshTokenRepository;
import com.codereview.auth.core.repository.UserRepository;
import com.think.platform.shared.common.exception.BusinessException;
import com.think.platform.shared.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Token Service - Auth Service
 *
 * 负责 JWT Token 和 Refresh Token 管理
 *
 * @author Auth Service Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${auth.refresh-token-expiry-days:7}")
    private int refreshTokenExpiryDays;

    /**
     * 生成访问令牌
     */
    public String generateAccessToken(User user) {
        return jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername());
    }

    /**
     * 生成刷新令牌
     */
    @Transactional
    public String generateRefreshToken(User user) {
        // 使该用户之前的刷新令牌失效
        refreshTokenRepository.revokeAllUserTokens(user.getId());

        // 创建新令牌
        String token = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpiryDays))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        log.info("Refresh token generated for user: {}", user.getUsername());

        return token;
    }

    /**
     * 验证刷新令牌
     */
    public RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("刷新令牌不存在"));

        if (!refreshToken.isValid()) {
            throw new BusinessException("刷新令牌已失效或已过期");
        }

        return refreshToken;
    }

    /**
     * 使用刷新令牌获取新的访问令牌
     */
    @Transactional
    public String refreshAccessToken(String refreshTokenStr) {
        RefreshToken refreshToken = validateRefreshToken(refreshTokenStr);
        User user = refreshToken.getUser();

        // 生成新的访问令牌
        String newAccessToken = generateAccessToken(user);
        log.info("Access token refreshed for user: {}", user.getUsername());

        return newAccessToken;
    }

    /**
     * 撤销刷新令牌
     */
    @Transactional
    public void revokeRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("刷新令牌不存在"));

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        log.info("Refresh token revoked for user: {}", refreshToken.getUser().getUsername());
    }

    /**
     * 撤销用户的所有刷新令牌
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllUserTokens(userId);
        log.info("All refresh tokens revoked for user ID: {}", userId);
    }

    /**
     * 清理过期的令牌
     */
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Expired refresh tokens cleaned up");
    }

    /**
     * 获取用户的活动刷新令牌数量
     */
    public long getActiveTokenCount(Long userId) {
        return refreshTokenRepository.countActiveTokensByUserId(userId, LocalDateTime.now());
    }
}
