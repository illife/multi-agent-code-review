package com.codereview.ai.domain.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Code Hash Calculator
 *
 * Calculates normalized SHA-256 hash of code content for caching purposes.
 * Normalization removes whitespace differences to identify functionally identical code.
 *
 * @author Code Review AI Team
 */
@Slf4j
@Component
public class CodeHashCalculator {

    /**
     * Calculate normalized SHA-256 hash of code.
     * Normalization: removes extra whitespace, standardizes line endings.
     *
     * @param code The code to hash
     * @return Base64-encoded SHA-256 hash
     */
    public String calculateHash(String code) {
        if (code == null || code.isEmpty()) {
            return "";
        }

        String normalized = normalizeCode(code);
        return sha256(normalized);
    }

    /**
     * Calculate hash using a custom normalization strategy.
     *
     * @param code The code to hash
     * @param aggressive Whether to use aggressive normalization (ignores more differences)
     * @return Base64-encoded SHA-256 hash
     */
    public String calculateHash(String code, boolean aggressive) {
        if (code == null || code.isEmpty()) {
            return "";
        }

        String normalized = aggressive ? normalizeAggressive(code) : normalizeCode(code);
        return sha256(normalized);
    }

    /**
     * Standard code normalization.
     * - Removes trailing whitespace
     * - Standardizes line endings to \n
     * - Collapses multiple consecutive spaces to single space
     */
    private String normalizeCode(String code) {
        return code
                // Standardize line endings
                .replaceAll("\\r\\n", "\n")
                .replaceAll("\\r", "\n")
                // Remove trailing whitespace from each line
                .replaceAll("[ \t]+$", "")
                // Collapse multiple consecutive spaces to single space (but preserve indentation)
                .replaceAll("  +", " ")
                // Trim final result
                .trim();
    }

    /**
     * Aggressive code normalization for looser matching.
     * - All standard normalizations
     * - Removes all leading/trailing whitespace
     * - Collapses all whitespace sequences to single space
     */
    private String normalizeAggressive(String code) {
        return code
                // Standardize line endings first
                .replaceAll("\\r\\n", "\n")
                .replaceAll("\\r", "\n")
                // Replace all whitespace sequences with single space
                .replaceAll("\\s+", " ")
                // Trim result
                .trim();
    }

    /**
     * Calculate SHA-256 hash and return Base64-encoded result.
     */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            // Fallback to simple hash
            return String.valueOf(input.hashCode());
        }
    }

    /**
     * Calculate a quick hash for comparison (not cryptographically secure).
     * Uses Java's built-in hashCode for fast comparison.
     */
    public int quickHash(String code) {
        if (code == null || code.isEmpty()) {
            return 0;
        }
        return normalizeCode(code).hashCode();
    }
}
