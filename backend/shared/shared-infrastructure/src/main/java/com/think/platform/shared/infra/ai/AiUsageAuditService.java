package com.think.platform.shared.infra.ai;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Redis-backed audit log for AI calls made by this application.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.usage-audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiUsageAuditService {

    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final long RETENTION_DAYS = 14;
    private static final ThreadLocal<AuditContext> AUDIT_CONTEXT = new ThreadLocal<>();

    private final RedisTemplate<String, Object> redisTemplate;

    public static Scope withContext(String principal, String feature) {
        AuditContext previous = AUDIT_CONTEXT.get();
        AUDIT_CONTEXT.set(new AuditContext(principal, feature));
        return new Scope(previous);
    }

    public void record(AiUsageAuditEvent event) {
        if (event == null) {
            return;
        }

        String date = todayUtc();
        String provider = normalize(event.provider(), "unknown");
        String model = normalize(event.model(), "unknown");
        String feature = normalize(firstNonBlank(currentFeature(), event.feature()), "general");
        String principal = normalize(firstNonBlank(event.principal(), currentPrincipal(), AiUsagePrincipalResolver.currentPrincipal()), "anonymous");
        long promptTokens = Math.max(0, event.promptTokens());
        long completionTokens = Math.max(0, event.completionTokens());
        long totalTokens = Math.max(0, event.totalTokens());
        if (totalTokens == 0) {
            totalTokens = promptTokens + completionTokens;
        }

        try {
            incrementCounter(siteKey(date), event.success(), promptTokens, completionTokens, totalTokens, event.durationMs());
            incrementCounter(modelKey(date, provider, model), event.success(), promptTokens, completionTokens, totalTokens, event.durationMs());
            incrementCounter(featureKey(date, feature), event.success(), promptTokens, completionTokens, totalTokens, event.durationMs());
            incrementCounter(principalKey(date, principal), event.success(), promptTokens, completionTokens, totalTokens, event.durationMs());

            addIndex(indexKey(date, "models"), provider + "/" + model);
            addIndex(indexKey(date, "features"), feature);
            addIndex(indexKey(date, "principals"), principal);

            log.info("AI_USAGE_AUDIT provider={} model={} feature={} principal={} totalTokens={} promptTokens={} completionTokens={} success={} durationMs={}",
                    provider, model, feature, principal, totalTokens, promptTokens, completionTokens, event.success(), event.durationMs());
        } catch (Exception e) {
            log.warn("Failed to record AI usage audit: provider={}, model={}, feature={}, reason={}",
                    provider, model, feature, e.getMessage());
        }
    }

    public Map<String, Object> summaryForDate(String date) {
        String safeDate = validateDate(date);
        Map<String, Object> summary = new HashMap<>();
        summary.put("date", safeDate);
        summary.put("site", counter(siteKey(safeDate)));
        summary.put("models", indexedCounters(safeDate, "models", value -> {
            String[] parts = value.split("/", 2);
            if (parts.length == 2) {
                return modelKey(safeDate, parts[0], parts[1]);
            }
            return modelKey(safeDate, "unknown", value);
        }));
        summary.put("features", indexedCounters(safeDate, "features", value -> featureKey(safeDate, value)));
        summary.put("principals", indexedCounters(safeDate, "principals", value -> principalKey(safeDate, value)));
        return summary;
    }

    public Map<String, Object> todaySummary() {
        return summaryForDate(todayUtc());
    }

    public static int estimateTokens(String text) {
        return AiUsageLimiter.estimateTokens(text);
    }

    private void incrementCounter(String key,
                                  boolean success,
                                  long promptTokens,
                                  long completionTokens,
                                  long totalTokens,
                                  long durationMs) {
        redisTemplate.opsForHash().increment(key, "calls", 1L);
        redisTemplate.opsForHash().increment(key, success ? "success" : "failure", 1L);
        redisTemplate.opsForHash().increment(key, "promptTokens", promptTokens);
        redisTemplate.opsForHash().increment(key, "completionTokens", completionTokens);
        redisTemplate.opsForHash().increment(key, "totalTokens", totalTokens);
        redisTemplate.opsForHash().increment(key, "durationMs", Math.max(0, durationMs));
        redisTemplate.expire(key, RETENTION_DAYS, TimeUnit.DAYS);
    }

    private void addIndex(String key, String value) {
        redisTemplate.opsForSet().add(key, value);
        redisTemplate.expire(key, RETENTION_DAYS, TimeUnit.DAYS);
    }

    private Map<String, Long> counter(String key) {
        Map<Object, Object> raw = redisTemplate.opsForHash().entries(key);
        Map<String, Long> result = new TreeMap<>();
        raw.forEach((field, value) -> result.put(String.valueOf(field), toLong(value)));
        result.putIfAbsent("calls", 0L);
        result.putIfAbsent("success", 0L);
        result.putIfAbsent("failure", 0L);
        result.putIfAbsent("promptTokens", 0L);
        result.putIfAbsent("completionTokens", 0L);
        result.putIfAbsent("totalTokens", 0L);
        result.putIfAbsent("durationMs", 0L);
        return result;
    }

    private List<Map<String, Object>> indexedCounters(String date, String indexName, KeyResolver keyResolver) {
        Set<Object> members = redisTemplate.opsForSet().members(indexKey(date, indexName));
        if (members == null || members.isEmpty()) {
            return List.of();
        }

        return members.stream()
                .map(String::valueOf)
                .map(value -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("name", value);
                    row.putAll(counter(keyResolver.resolve(value)));
                    return row;
                })
                .sorted(Comparator.comparingLong(row -> -toLong(row.get("totalTokens"))))
                .toList();
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String validateDate(String date) {
        if (date == null || !DATE_PATTERN.matcher(date).matches()) {
            return todayUtc();
        }
        return date;
    }

    private String todayUtc() {
        return LocalDate.now(ZoneOffset.UTC).toString();
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9:_./-]", "_");
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String currentPrincipal() {
        AuditContext context = AUDIT_CONTEXT.get();
        return context != null ? context.principal() : null;
    }

    private static String currentFeature() {
        AuditContext context = AUDIT_CONTEXT.get();
        return context != null ? context.feature() : null;
    }

    private String siteKey(String date) {
        return "ai:audit:" + date + ":site";
    }

    private String modelKey(String date, String provider, String model) {
        return "ai:audit:" + date + ":model:" + provider + ":" + model;
    }

    private String featureKey(String date, String feature) {
        return "ai:audit:" + date + ":feature:" + feature;
    }

    private String principalKey(String date, String principal) {
        return "ai:audit:" + date + ":principal:" + principal;
    }

    private String indexKey(String date, String name) {
        return "ai:audit:" + date + ":index:" + name;
    }

    private interface KeyResolver {
        String resolve(String value);
    }

    public static final class Scope implements AutoCloseable {
        private final AuditContext previous;

        private Scope(AuditContext previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (previous == null) {
                AUDIT_CONTEXT.remove();
            } else {
                AUDIT_CONTEXT.set(previous);
            }
        }
    }

    private record AuditContext(String principal, String feature) {
    }

    @Builder
    public record AiUsageAuditEvent(
            String provider,
            String model,
            String feature,
            String principal,
            long promptTokens,
            long completionTokens,
            long totalTokens,
            long durationMs,
            boolean success
    ) {
    }
}
