package com.company.kb.infra.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 登录尝试服务
 * 用于防止暴力破解攻击
 *
 * 功能：
 * 1. 记录用户登录失败次数
 * 2. 记录IP地址登录失败次数
 * 3. 自动锁定账户（达到失败次数上限）
 * 4. 自动锁定IP（达到失败次数上限）
 * 5. 过期后自动解锁
 *
 * @author Knowledge Base Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final RedisTemplate<String, Object> redisTemplate;

    // 配置参数
    private static final int MAX_ATTEMPTS = 5;           // 最大失败次数
    private static final long LOCK_TIME = 30 * 60 * 1000; // 锁定时间：30分钟（毫秒）
    private static final long ATTEMPT_TTL = 15 * 60 * 1000; // 失败记录过期时间：15分钟（毫秒）

    private static final String LOGIN_ATTEMPT_PREFIX = "login:attempt:";
    private static final String LOGIN_LOCK_PREFIX = "login:lock:";
    private static final String IP_ATTEMPT_PREFIX = "ip:attempt:";
    private static final String IP_LOCK_PREFIX = "ip:lock:";

    /**
     * 记录登录失败
     *
     * @param username 用户名
     * @param ip IP地址
     */
    public void loginFailed(String username, String ip) {
        // 记录用户失败次数
        String userKey = LOGIN_ATTEMPT_PREFIX + username;
        Long userAttempts = redisTemplate.opsForValue().increment(userKey);

        if (userAttempts == 1) {
            // 第一次失败，设置过期时间
            redisTemplate.expire(userKey, ATTEMPT_TTL, TimeUnit.MILLISECONDS);
        }

        if (userAttempts >= MAX_ATTEMPTS) {
            // 达到最大失败次数，锁定用户
            lockUser(username);
            log.warn("用户因登录失败次数过多被锁定: username={}, attempts={}", username, userAttempts);
        }

        // 记录IP失败次数
        String ipKey = IP_ATTEMPT_PREFIX + ip;
        Long ipAttempts = redisTemplate.opsForValue().increment(ipKey);

        if (ipAttempts == 1) {
            redisTemplate.expire(ipKey, ATTEMPT_TTL, TimeUnit.MILLISECONDS);
        }

        if (ipAttempts >= MAX_ATTEMPTS) {
            // 达到最大失败次数，锁定IP
            lockIp(ip);
            log.warn("IP因登录失败次数过多被锁定: ip={}, attempts={}", ip, ipAttempts);
        }

        log.debug("登录失败记录: username={}, ip={}, userAttempts={}, ipAttempts={}",
            username, ip, userAttempts, ipAttempts);
    }

    /**
     * 登录成功，清除失败记录
     *
     * @param username 用户名
     * @param ip IP地址
     */
    public void loginSucceeded(String username, String ip) {
        // 清除用户失败记录
        String userKey = LOGIN_ATTEMPT_PREFIX + username;
        redisTemplate.delete(userKey);

        // 清除IP失败记录
        String ipKey = IP_ATTEMPT_PREFIX + ip;
        redisTemplate.delete(ipKey);

        log.debug("登录成功，清除失败记录: username={}, ip={}", username, ip);
    }

    /**
     * 检查用户是否被锁定
     *
     * @param username 用户名
     * @return true如果被锁定，false否则
     */
    public boolean isUserLocked(String username) {
        String lockKey = LOGIN_LOCK_PREFIX + username;
        Boolean isLocked = redisTemplate.hasKey(lockKey);
        return isLocked != null && isLocked;
    }

    /**
     * 检查IP是否被锁定
     *
     * @param ip IP地址
     * @return true如果被锁定，false否则
     */
    public boolean isIpLocked(String ip) {
        String lockKey = IP_LOCK_PREFIX + ip;
        Boolean isLocked = redisTemplate.hasKey(lockKey);
        return isLocked != null && isLocked;
    }

    /**
     * 获取用户剩余锁定时间（秒）
     *
     * @param username 用户名
     * @return 剩余锁定时间（秒），-1表示未锁定
     */
    public long getUserLockTimeRemaining(String username) {
        String lockKey = LOGIN_LOCK_PREFIX + username;
        Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : -1;
    }

    /**
     * 获取IP剩余锁定时间（秒）
     *
     * @param ip IP地址
     * @return 剩余锁定时间（秒），-1表示未锁定
     */
    public long getIpLockTimeRemaining(String ip) {
        String lockKey = IP_LOCK_PREFIX + ip;
        Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : -1;
    }

    /**
     * 锁定用户
     */
    private void lockUser(String username) {
        String lockKey = LOGIN_LOCK_PREFIX + username;
        redisTemplate.opsForValue().set(lockKey, "locked", LOCK_TIME, TimeUnit.MILLISECONDS);
    }

    /**
     * 锁定IP
     */
    private void lockIp(String ip) {
        String lockKey = IP_LOCK_PREFIX + ip;
        redisTemplate.opsForValue().set(lockKey, "locked", LOCK_TIME, TimeUnit.MILLISECONDS);
    }

    /**
     * 手动解锁用户（管理员功能）
     *
     * @param username 用户名
     */
    public void unlockUser(String username) {
        String lockKey = LOGIN_LOCK_PREFIX + username;
        String attemptKey = LOGIN_ATTEMPT_PREFIX + username;

        redisTemplate.delete(lockKey);
        redisTemplate.delete(attemptKey);

        log.info("用户已手动解锁: username={}", username);
    }

    /**
     * 手动解锁IP（管理员功能）
     *
     * @param ip IP地址
     */
    public void unlockIp(String ip) {
        String lockKey = IP_LOCK_PREFIX + ip;
        String attemptKey = IP_ATTEMPT_PREFIX + ip;

        redisTemplate.delete(lockKey);
        redisTemplate.delete(attemptKey);

        log.info("IP已手动解锁: ip={}", ip);
    }
}
