package com.codereview.ai.domain.agent.rule.impl;

import com.codereview.ai.domain.agent.rule.ReviewRule;
import com.codereview.ai.domain.model.CodeIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Weak Cryptography Detection Rule
 *
 * Detects use of weak cryptographic algorithms and practices.
 *
 * @author Code Review AI Team
 */
@Slf4j
@Component
public class WeakCryptoRule implements ReviewRule {

    // Pattern 1: Weak algorithms (MD5, SHA1, DES, RC4)
    private static final Pattern WEAK_ALGORITHM_PATTERN = Pattern.compile(
        "(MD5|SHA1|SHA-1|DES|RC4| ECB)\\b",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern 2: Default encryption parameters
    private static final Pattern DEFAULT_CRYPTO_PATTERN = Pattern.compile(
        "(Cipher\\s*\\(\\s*\\)|encrypt\\s*\\(\\s*\\)|decrypt\\s*\\(\\s*\\))",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern 3: Hardcoded IV
    private static final Pattern HARDCODED_IV_PATTERN = Pattern.compile(
        "(IV|iv|initializationVector)\\s*[=:]\\s*[\"'][^\"']{8,}[\"']",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public String getName() {
        return "WEAK_CRYPTO";
    }

    @Override
    public String getCategory() {
        return "security";
    }

    @Override
    public CodeIssue.Severity getSeverity() {
        return CodeIssue.Severity.HIGH;
    }

    @Override
    public boolean supportsLanguage(String language) {
        return List.of("java", "javascript", "typescript", "python", "go", "c#", "php", "ruby")
                .contains(language.toLowerCase());
    }

    @Override
    public List<CodeIssue> check(String code, Long reviewId) {
        List<CodeIssue> issues = new ArrayList<>();

        if (WEAK_ALGORITHM_PATTERN.matcher(code).find()) {
            int line = findLineNumber(code, WEAK_ALGORITHM_PATTERN);
            issues.add(CodeIssue.builder()
                    .reviewId(reviewId)
                    .severity(CodeIssue.Severity.HIGH)
                    .category("security")
                    .title("Weak cryptographic algorithm")
                    .description("Code uses weak cryptographic algorithm (MD5, SHA1, DES, RC4)")
                    .lineNumber(line)
                    .suggestion("Use strong algorithms like SHA-256, SHA-512, or AES-256")
                    .ruleId(getName())
                    .agentType("STATIC_ANALYSIS")
                    .toolName("RuleEngine")
                    .build());
        }

        if (HARDCODED_IV_PATTERN.matcher(code).find()) {
            int line = findLineNumber(code, HARDCODED_IV_PATTERN);
            issues.add(CodeIssue.builder()
                    .reviewId(reviewId)
                    .severity(CodeIssue.Severity.MEDIUM)
                    .category("security")
                    .title("Hardcoded initialization vector")
                    .description("Initialization vector (IV) should be random, not hardcoded")
                    .lineNumber(line)
                    .suggestion("Generate a random IV for each encryption operation")
                    .ruleId("HARDCODED_IV")
                    .agentType("STATIC_ANALYSIS")
                    .toolName("RuleEngine")
                    .build());
        }

        return issues;
    }
}
