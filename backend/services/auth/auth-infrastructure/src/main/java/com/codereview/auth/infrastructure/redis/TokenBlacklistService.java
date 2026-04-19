package com.think.platform.ci.auth.infrastructure.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token Blacklist Service - Redis
 *
 * Manages token blacklist for logout functionality
 *
 * @author Auth Service Team
 */
@Slf4j
@Service
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private static final String USER_BLACKLIST_PREFIX = "token:blacklist:user:";

    private final RedisTemplate<String, String> redisTemplate;

    public TokenBlacklistService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Add token to blacklist
     *
     * @param token JWT token
     * @param ttlMillis Time to live in milliseconds
     */
    public void addToBlacklist(String token, long ttlMillis) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "true", ttlMillis, TimeUnit.MILLISECONDS);
        log.debug("Token added to blacklist: ttl={}ms", ttlMillis);
    }

    /**
     * Check if token is blacklisted
     *
     * @param token JWT token
     * @return true if blacklisted
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Blacklist all tokens for a user
     *
     * @param userId User ID
     * @param ttlMillis Time to live for user blacklist entry
     */
    public void blacklistAllUserTokens(Long userId, long ttlMillis) {
        String key = USER_BLACKLIST_PREFIX + userId;
        redisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()), ttlMillis, TimeUnit.MILLISECONDS);
        log.info("All tokens blacklisted for user: userId={}", userId);
    }

    /**
     * Check if user's tokens are blacklisted
     *
     * @param userId User ID
     * @param tokenIssuedAt Token issue timestamp (milliseconds)
     * @return true if user tokens are blacklisted after token was issued
     */
    public boolean areUserTokensBlacklisted(Long userId, long tokenIssuedAt) {
        String key = USER_BLACKLIST_PREFIX + userId;
        String blacklistTime = redisTemplate.opsForValue().get(key);

        if (blacklistTime != null) {
            try {
                long blacklistTimestamp = Long.parseLong(blacklistTime);
                return tokenIssuedAt < blacklistTimestamp;
            } catch (NumberFormatException e) {
                log.warn("Invalid blacklist timestamp for user: userId={}", userId);
            }
        }

        return false;
    }

    /**
     * Remove token from blacklist (for testing)
     *
     * @param token JWT token
     */
    public void removeFromBlacklist(String token) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.delete(key);
        log.debug("Token removed from blacklist");
    }

    /**
     * Clear user token blacklist
     *
     * @param userId User ID
     */
    public void clearUserBlacklist(Long userId) {
        String key = USER_BLACKLIST_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("User token blacklist cleared: userId={}", userId);
    }
}
