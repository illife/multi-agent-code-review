package com.codereview.auth.core.service;

import com.codereview.auth.core.domain.EmailVerificationToken;
import com.codereview.auth.core.domain.User;
import com.codereview.auth.core.repository.EmailVerificationTokenRepository;
import com.codereview.auth.core.repository.UserRepository;
import com.think.platform.shared.common.exception.BusinessException;
import com.think.platform.shared.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Handles account email verification tokens.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

    @Value("${auth.email.verification-token-expiry-hours:48}")
    private int verificationTokenExpiryHours;

    @Transactional
    public String generateVerificationToken(User user) {
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            log.info("Email already verified for user: {}", user.getUsername());
            return null;
        }

        tokenRepository.invalidateAllUserTokens(user.getId());

        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(verificationTokenExpiryHours))
                .used(false)
                .build();

        tokenRepository.save(verificationToken);
        log.info("Email verification token generated for user: {}", user.getUsername());
        return token;
    }

    @Transactional
    public User verifyEmail(String token) {
        EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("邮箱验证链接不存在"));

        if (!verificationToken.isValid()) {
            throw new BusinessException("邮箱验证链接已失效或已过期");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);

        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);

        log.info("Email verified for user: {}", user.getUsername());
        return user;
    }

    @Transactional
    public String regenerateVerificationToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        return generateVerificationToken(user);
    }

    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteExpiredOrUsedTokens(LocalDateTime.now());
        log.info("Expired email verification tokens cleaned up");
    }
}
