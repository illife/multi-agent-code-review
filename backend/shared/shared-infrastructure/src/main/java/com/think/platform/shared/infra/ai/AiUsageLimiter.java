package com.think.platform.shared.infra.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

/**
 * Redis-backed guardrail for paid AI calls.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.usage-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiUsageLimiter {

    private static final int MIN_ESTIMATED_TOKENS = 1;

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${ai.usage-limit.daily-token-budget:120000}")
    private long dailyTokenBudget;

    @Value("${ai.usage-limit.site-daily-token-budget:800000}")
    private long siteDailyTokenBudget;

    @Value("${ai.usage-limit.minute-call-limit:8}")
    private long minuteCallLimit;

    @Value("${ai.usage-limit.fail-open:false}")
    private boolean failOpen;

    public void checkAndConsume(Long userId, String feature, int estimatedTokens) {
        String principal = userId != null ? "user:" + userId : "anonymous";
        checkAndConsume(principal, feature, estimatedTokens);
    }

    public void checkAndConsume(String principal, String feature, int estimatedTokens) {
        String safePrincipal = normalizePrincipal(principal);
        String safeFeature = normalize(feature);
        int safeEstimatedTokens = Math.max(estimatedTokens, MIN_ESTIMATED_TOKENS);

        try {
            checkMinuteCalls(safePrincipal, safeFeature);
            checkDailyBudget(safePrincipal, safeFeature, safeEstimatedTokens);
            checkSiteDailyBudget(safePrincipal, safeFeature, safeEstimatedTokens);
        } catch (AiUsageLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            if (failOpen) {
                log.warn("AI usage limiter failed open: principal={}, feature={}, reason={}",
                        safePrincipal, safeFeature, e.getMessage());
                return;
            }
            throw new AiUsageLimitExceededException("AI 使用限制检查失败，请稍后重试");
        }
    }

    private void checkMinuteCalls(String principal, String feature) {
        String key = "ai:usage:minute:" + todayUtc() + ":principal:" + principal + ":feature:" + feature;
        Long calls = redisTemplate.opsForValue().increment(key);
        if (calls != null && calls == 1L) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }
        if (minuteCallLimit > 0 && calls != null && calls > minuteCallLimit) {
            throw new AiUsageLimitExceededException("AI 调用过于频繁，请 1 分钟后再试");
        }
    }

    private void checkDailyBudget(String principal, String feature, int estimatedTokens) {
        String key = "ai:usage:daily:" + todayUtc() + ":principal:" + principal;
        Long used = redisTemplate.opsForValue().increment(key, estimatedTokens);
        if (used != null && used == estimatedTokens) {
            redisTemplate.expire(key, secondsUntilNextUtcDay(), TimeUnit.SECONDS);
        }
        if (dailyTokenBudget > 0 && used != null && used > dailyTokenBudget) {
            log.warn("AI daily budget exceeded: principal={}, feature={}, used={}, budget={}",
                    principal, feature, used, dailyTokenBudget);
            throw new AiUsageLimitExceededException("今日 AI 使用额度已用完，请明天再试");
        }
    }

    private void checkSiteDailyBudget(String principal, String feature, int estimatedTokens) {
        String key = "ai:usage:daily:" + todayUtc() + ":site";
        Long used = redisTemplate.opsForValue().increment(key, estimatedTokens);
        if (used != null && used == estimatedTokens) {
            redisTemplate.expire(key, secondsUntilNextUtcDay(), TimeUnit.SECONDS);
        }
        if (siteDailyTokenBudget > 0 && used != null && used > siteDailyTokenBudget) {
            log.warn("AI site daily budget exceeded: principal={}, feature={}, used={}, budget={}",
                    principal, feature, used, siteDailyTokenBudget);
            throw new AiUsageLimitExceededException("今日站点 AI 额度已用完，请明天再试");
        }
    }

    private long secondsUntilNextUtcDay() {
        long now = java.time.Instant.now().getEpochSecond();
        long nextDay = todayUtc().plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        return Math.max(60, nextDay - now);
    }

    private LocalDate todayUtc() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    private String normalizePrincipal(String principal) {
        if (principal == null || principal.isBlank()) {
            return "anonymous";
        }
        return normalize(principal);
    }

    private String normalize(String feature) {
        if (feature == null || feature.isBlank()) {
            return "general";
        }
        return feature.replaceAll("[^a-zA-Z0-9:_-]", "_");
    }

    public static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        int chineseChars = 0;
        int otherChars = 0;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN) {
                chineseChars++;
            } else {
                otherChars++;
            }
            i += Character.charCount(codePoint);
        }

        return (int) Math.ceil(chineseChars / 1.5 + otherChars / 4.0);
    }
}
