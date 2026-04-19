package com.company.kb.config;

import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 使用Guava RateLimiter实现的API访问频率限制配置
 *
 * 为不同类型的API端点提供不同的频率限制：
 * - 认证端点：严格限制（10次/分钟）
 * - 普通API端点：中等限制（100次/分钟）
 * - 只读端点：宽松限制（200次/分钟）
 *
 * @author hjy
 */
@Slf4j
@Component
public class RateLimitConfig {

    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${app.rate-limit.auth.permits-per-second:0.17}")
    private double authPermitsPerSecond; // 10次/分钟 ≈ 0.17次/秒

    @Value("${app.rate-limit.api.permits-per-second:1.67}")
    private double apiPermitsPerSecond; // 100次/分钟 ≈ 1.67次/秒

    @Value("${app.rate-limit.read.permits-per-second:3.33}")
    private double readPermitsPerSecond; // 200次/分钟 ≈ 3.33次/秒

    // 存储每个客户端的限流器：客户端ID -> RateLimiter
    private final Map<String, RateLimiter> authLimiters = new ConcurrentHashMap<>();
    private final Map<String, RateLimiter> apiLimiters = new ConcurrentHashMap<>();
    private final Map<String, RateLimiter> readLimiters = new ConcurrentHashMap<>();

    /**
     * 获取或创建认证端点的频率限制器
     * 使用限制：每分钟10次请求
     *
     * @param clientId 客户端标识
     * @return RateLimiter实例
     */
    public RateLimiter getAuthLimiter(String clientId) {
        if (!rateLimitEnabled) {
            return RateLimiter.create(Double.MAX_VALUE);
        }

        return authLimiters.computeIfAbsent(clientId, key -> {
            log.debug("为客户端创建新的认证频率限制器: {}", key);
            return RateLimiter.create(authPermitsPerSecond);
        });
    }

    /**
     * 获取或创建普通API端点的频率限制器
     * 使用限制：每分钟100次请求
     *
     * @param clientId 客户端标识
     * @return RateLimiter实例
     */
    public RateLimiter getApiLimiter(String clientId) {
        if (!rateLimitEnabled) {
            return RateLimiter.create(Double.MAX_VALUE);
        }

        return apiLimiters.computeIfAbsent(clientId, key -> {
            log.debug("为客户端创建新的API频率限制器: {}", key);
            return RateLimiter.create(apiPermitsPerSecond);
        });
    }

    /**
     * 获取或创建只读端点的频率限制器
     * 使用限制：每分钟200次请求
     *
     * @param clientId 客户端标识
     * @return RateLimiter实例
     */
    public RateLimiter getReadLimiter(String clientId) {
        if (!rateLimitEnabled) {
            return RateLimiter.create(Double.MAX_VALUE);
        }

        return readLimiters.computeIfAbsent(clientId, key -> {
            log.debug("为客户端创建新的只读频率限制器: {}", key);
            return RateLimiter.create(readPermitsPerSecond);
        });
    }

    /**
     * 清理过期的限制器，防止内存泄漏
     * 应该定期调用（例如通过定时任务）
     */
    public void cleanupExpiredLimiters() {
        // 记录当前限流器数量
        log.debug("频率限制器统计 - 认证: {}, API: {}, 只读: {}",
            authLimiters.size(), apiLimiters.size(), readLimiters.size());

        // TODO: 可以实现基于时间的清理逻辑，移除长时间未使用的限流器
    }

    /**
     * 从请求中解析客户端标识符
     * 优先级：用户ID > IP地址 > 未知
     *
     * @param userId 用户ID
     * @param ipAddress IP地址
     * @return 客户端标识字符串
     */
    public String resolveClientId(String userId, String ipAddress) {
        if (userId != null && !userId.isEmpty()) {
            return "user:" + userId;
        }
        if (ipAddress != null && !ipAddress.isEmpty()) {
            return "ip:" + ipAddress;
        }
        return "unknown";
    }

    /**
     * 检查频率限制是否启用
     *
     * @return true表示启用，false表示禁用
     */
    public boolean isRateLimitEnabled() {
        return rateLimitEnabled;
    }

    /**
     * 尝试获取访问许可（非阻塞）
     *
     * @param limiter 限流器
     * @return true表示获取成功，false表示已被限流
     */
    public boolean tryAcquire(RateLimiter limiter) {
        return limiter.tryAcquire();
    }
}