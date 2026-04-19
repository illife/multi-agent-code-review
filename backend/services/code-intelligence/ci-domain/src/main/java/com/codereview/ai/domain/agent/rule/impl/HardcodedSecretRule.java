package com.codereview.ai.domain.agent.rule.impl;

import com.codereview.ai.domain.agent.rule.ReviewRule;
import com.codereview.ai.domain.model.CodeIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Hardcoded Secret Detection Rule
 *
 * Detects hardcoded secrets, passwords, API keys, and tokens in code.
 *
 * @author Code Review AI Team
 */
@Slf4j
@Component
public class HardcodedSecretRule implements ReviewRule {

    // Pattern 1: Hardcoded passwords, API keys, secrets
    private static final Pattern SECRET_PATTERN = Pattern.compile(
        "(password|api_key|apikey|secret|token|private_key|auth_token|access_token)\\s*[=:]\\s*[\"'][^\"']{8,}[\"']",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern 2: Base64-like strings that could be secrets
    private static final Pattern BASE64_SECRET_PATTERN = Pattern.compile(
        "[\"'][A-Za-z0-9+/]{32,}={0,2}[\"']"
    );

    // Pattern 3: JWT-like tokens
    private static final Pattern JWT_PATTERN = Pattern.compile(
        "[\"'][A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+[\"']"
    );

    // Pattern 4: AWS keys
    private static final Pattern AWS_KEY_PATTERN = Pattern.compile(
        "(AKIA[0-9A-Z]{16})"
    );

    // Pattern 5: Generic API key patterns
    private static final Pattern API_KEY_PATTERN = Pattern.compile(
        "(api[_-]?key|apikey|access[_-]?key)\\s*[=:]\\s*[\"']?[a-zA-Z0-9_\\-]{20,}"
    );

    @Override
    public String getName() {
        return "HARDCODED_SECRET";
    }

    @Override
    public String getCategory() {
        return "security";
    }

    @Override
    public CodeIssue.Severity getSeverity() {
        return CodeIssue.Severity.CRITICAL;
    }

    @Override
    public boolean supportsLanguage(String language) {
        return true; // Applies to all languages
    }

    @Override
    public List<CodeIssue> check(String code, Long reviewId) {
        List<CodeIssue> issues = new ArrayList<>();

        // Check for hardcoded secrets
        if (SECRET_PATTERN.matcher(code).find()) {
            int line = findLineNumber(code, SECRET_PATTERN);
            issues.add(CodeIssue.builder()
                    .reviewId(reviewId)
                    .severity(CodeIssue.Severity.CRITICAL)
                    .category("security")
                    .title("Hardcoded secret detected")
                    .description("Code contains hardcoded credentials, API keys, or secrets")
                    .lineNumber(line)
                    .suggestion("Move secrets to environment variables or secure configuration management")
                    .ruleId(getName())
                    .agentType("STATIC_ANALYSIS")
                    .toolName("RuleEngine")
                    .build());
        }

        // Check for AWS access keys
        if (AWS_KEY_PATTERN.matcher(code).find()) {
            int line = findLineNumber(code, AWS_KEY_PATTERN);
            issues.add(CodeIssue.builder()
                    .reviewId(reviewId)
                    .severity(CodeIssue.Severity.CRITICAL)
                    .category("security")
                    .title("AWS access key detected")
                    .description("Code contains what appears to be an AWS access key ID")
                    .lineNumber(line)
                    .suggestion("Remove the AWS key and use IAM roles or environment variables")
                    .ruleId("AWS_KEY")
                    .agentType("STATIC_ANALYSIS")
                    .toolName("RuleEngine")
                    .build());
        }

        // Check for JWT tokens
        if (JWT_PATTERN.matcher(code).find()) {
            int line = findLineNumber(code, JWT_PATTERN);
            issues.add(CodeIssue.builder()
                    .reviewId(reviewId)
                    .severity(CodeIssue.Severity.HIGH)
                    .category("security")
                    .title("Hardcoded JWT token")
                    .description("Code contains what appears to be a hardcoded JWT token")
                    .lineNumber(line)
                    .suggestion("Tokens should be obtained dynamically, not hardcoded")
                    .ruleId("HARDCODED_JWT")
                    .agentType("STATIC_ANALYSIS")
                    .toolName("RuleEngine")
                    .build());
        }

        // Check for API key patterns
        if (API_KEY_PATTERN.matcher(code).find()) {
            int line = findLineNumber(code, API_KEY_PATTERN);
            issues.add(CodeIssue.builder()
                    .reviewId(reviewId)
                    .severity(CodeIssue.Severity.HIGH)
                    .category("security")
                    .title("Potential hardcoded API key")
                    .description("Code contains a potential hardcoded API key")
                    .lineNumber(line)
                    .suggestion("Use environment variables or secure configuration for API keys")
                    .ruleId("API_KEY")
                    .agentType("STATIC_ANALYSIS")
                    .toolName("RuleEngine")
                    .build());
        }

        return issues;
    }
}
