package com.codereview.ai.domain.cache;

import com.codereview.ai.domain.model.CodeIssue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Review Result Cache
 *
 * Caches code review results to reduce redundant AI API calls.
 * Uses in-memory cache (Redis integration available when configured).
 *
 * @author Code Review AI Team
 */
@Slf4j
@Component
public class ReviewCache {

    private static final String CACHE_PREFIX = "review:cache:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    // In-memory fallback cache
    private final ConcurrentHashMap<String, CachedReviewResult> fallbackCache;

    /**
     * Constructor - uses in-memory cache
     */
    public ReviewCache() {
        this.fallbackCache = new ConcurrentHashMap<>();
        log.info("ReviewCache initialized with in-memory backend");
    }

    /**
     * Get cached review results for a code hash.
     *
     * @param codeHash The hash of the code
     * @param language The programming language
     * @return Optional list of issues if cached
     */
    public Optional<List<CodeIssue>> get(String codeHash, String language) {
        String key = buildKey(codeHash, language);

        // Use in-memory cache
        CachedReviewResult cached = fallbackCache.get(key);
        if (cached != null && !isExpired(cached)) {
            log.debug("Cache hit (memory) for key: {}", key);
            return Optional.of(cached.getIssues());
        }

        log.debug("Cache miss for key: {}", key);
        return Optional.empty();
    }

    /**
     * Cache review results for a code hash.
     *
     * @param codeHash The hash of the code
     * @param language The programming language
     * @param issues The list of issues to cache
     */
    public void put(String codeHash, String language, List<CodeIssue> issues) {
        String key = buildKey(codeHash, language);
        CachedReviewResult result = CachedReviewResult.builder()
                .codeHash(codeHash)
                .language(language)
                .issues(issues)
                .cachedAt(LocalDateTime.now())
                .issueCount(issues.size())
                .build();

        // Use in-memory cache
        fallbackCache.put(key, result);
        log.debug("Cached {} issues (memory) for key: {}", issues.size(), key);

        // Cleanup old entries periodically
        if (fallbackCache.size() > 1000) {
            cleanupExpiredEntries();
        }
    }

    /**
     * Invalidate cache for a specific code hash.
     *
     * @param codeHash The hash of the code
     * @param language The programming language
     */
    public void invalidate(String codeHash, String language) {
        String key = buildKey(codeHash, language);
        fallbackCache.remove(key);
        log.debug("Invalidated cache entry (memory): {}", key);
    }

    /**
     * Clear all cached review results.
     */
    public void clear() {
        int size = fallbackCache.size();
        fallbackCache.clear();
        log.info("Cleared all review cache entries (memory): {} entries", size);
    }

    /**
     * Get cache statistics.
     *
     * @return CacheStats object with hit/miss counts and sizes
     */
    public CacheStats getStats() {
        int memorySize = fallbackCache.size();

        return CacheStats.builder()
                .memorySize(memorySize)
                .totalSize(memorySize)
                .usingRedis(false)
                .build();
    }

    /**
     * Check if a cached result has expired.
     */
    private boolean isExpired(CachedReviewResult cached) {
        if (cached.getCachedAt() == null) {
            return false;
        }
        return cached.getCachedAt().plus(CACHE_TTL).isBefore(LocalDateTime.now());
    }

    /**
     * Build cache key from code hash and language.
     */
    private String buildKey(String codeHash, String language) {
        return CACHE_PREFIX + language + ":" + codeHash;
    }

    /**
     * Clean up expired cache entries.
     */
    private void cleanupExpiredEntries() {
        int beforeSize = fallbackCache.size();
        LocalDateTime now = LocalDateTime.now();

        fallbackCache.entrySet().removeIf(entry -> {
            CachedReviewResult cached = entry.getValue();
            if (cached.getCachedAt() == null) {
                return true;
            }
            return cached.getCachedAt().plus(CACHE_TTL).isBefore(now);
        });

        int cleaned = beforeSize - fallbackCache.size();
        if (cleaned > 0) {
            log.info("Cleaned up {} expired cache entries", cleaned);
        }
    }

    /**
     * Cached review result data structure.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CachedReviewResult {
        private String codeHash;
        private String language;
        private List<CodeIssue> issues;
        private LocalDateTime cachedAt;
        private int issueCount;
    }

    /**
     * Cache statistics.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CacheStats {
        private int memorySize;
        private int totalSize;
        private boolean usingRedis;
    }
}
