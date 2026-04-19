package com.company.kb.infra.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token黑名单服务
 * 用于实现真正的登出功能
 *
 * 工作原理：
 * 1. 用户登出时，将JWT Token加入Redis黑名单
 * 2. Token验证时，检查是否在黑名单中
 * 3. 黑名单有过期时间，与JWT过期时间一致
 * 4. 使用Redis是因为：
 *    - 快速查询
 *    - 自动过期
 *    - 分布式环境支持
 *
 * @author Knowledge Base Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private static final String USER_BLACKLIST_PREFIX = "user:tokens:blacklist:";

    /**
     * 将Token加入黑名单
     *
     * @param token JWT Token
     * @param expirationMs Token过期时间（毫秒）
     */
    public void addToBlacklist(String token, long expirationMs) {
        String key = BLACKLIST_PREFIX + token;

        // 设置黑名单，过期时间与JWT过期时间一致
        redisTemplate.opsForValue().set(key, "true", expirationMs, TimeUnit.MILLISECONDS);

        log.debug("Token已加入黑名单: {}, 过期时间: {}ms", token.substring(0, Math.min(20, token.length())), expirationMs);
    }

    /**
     * 检查Token是否在黑名单中
     *
     * @param token JWT Token
     * @return true如果在黑名单中，false否则
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(key);

        if (exists != null && exists) {
            log.debug("Token在黑名单中: {}", token.substring(0, Math.min(20, token.length())));
            return true;
        }

        return false;
    }

    /**
     * 将用户的所有Token加入黑名单（用于强制登出所有设备）
     *
     * @param userId 用户ID
     * @param expirationMs Token过期时间（毫秒）
     */
    public void blacklistAllUserTokens(Long userId, long expirationMs) {
        String key = USER_BLACKLIST_PREFIX + userId;

        // 设置用户黑名单时间戳
        redisTemplate.opsForValue().set(key, System.currentTimeMillis(), expirationMs, TimeUnit.MILLISECONDS);

        log.info("用户所有Token已加入黑名单: userId={}, 过期时间: {}ms", userId, expirationMs);
    }

    /**
     * 检查用户Token是否应该被作废（用于强制登出所有设备）
     *
     * @param userId 用户ID
     * @param tokenIssuedAt Token签发时间（毫秒）
     * @return true如果应该被作废，false否则
     */
    public boolean isUserTokenBlacklisted(Long userId, long tokenIssuedAt) {
        String key = USER_BLACKLIST_PREFIX + userId;
        Object blacklistTime = redisTemplate.opsForValue().get(key);

        if (blacklistTime != null) {
            long blacklistTimestamp = (long) blacklistTime;
            // 如果Token是在黑名单时间之前签发的，则应该被作废
            if (tokenIssuedAt < blacklistTimestamp) {
                log.debug("用户Token被作废: userId={}, tokenIssuedAt={}, blacklistTime={}",
                    userId, tokenIssuedAt, blacklistTimestamp);
                return true;
            }
        }

        return false;
    }

    /**
     * 从黑名单中移除Token（用于测试）
     *
     * @param token JWT Token
     */
    public void removeFromBlacklist(String token) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.delete(key);
        log.debug("Token已从黑名单移除: {}", token.substring(0, Math.min(20, token.length())));
    }
}
