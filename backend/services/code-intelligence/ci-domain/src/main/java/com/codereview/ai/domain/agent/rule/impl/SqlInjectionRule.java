package com.codereview.ai.domain.agent.rule.impl;

import com.codereview.ai.domain.agent.rule.ReviewRule;
import com.codereview.ai.domain.model.CodeIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * SQL Injection Detection Rule
 *
 * Detects potential SQL injection vulnerabilities in code.
 *
 * @author Code Review AI Team
 */
@Slf4j
@Component
public class SqlInjectionRule implements ReviewRule {

    // Pattern 1: String concatenation in SQL queries
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(\".*\\+.*\"|'.*\\+.*').*(SELECT|INSERT|UPDATE|DELETE|CREATE|ALTER|DROP)",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern 2: Direct user input in SQL without parameterization
    private static final Pattern DANGEROUS_SQL_PATTERN = Pattern.compile(
        "(Statement|executeQuery|executeUpdate|createQuery|nativeQuery|createNativeQuery)\\s*\\([^)]*\\+[^)]*\\)",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern 3: String formatting in SQL
    private static final Pattern FORMAT_SQL_PATTERN = Pattern.compile(
        "(String\\.format|MessageFormat\\.format|\\.format\\s*\\()[^)]*%(s|d)[^)]*\\s*(SELECT|INSERT|UPDATE|DELETE)",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public String getName() {
        return "SQL_INJECTION";
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
        return List.of("java", "javascript", "typescript", "python", "go", "php", "c#", "ruby")
                .contains(language.toLowerCase());
    }

    @Override
    public List<CodeIssue> check(String code, Long reviewId) {
        List<CodeIssue> issues = new ArrayList<>();

        // Check for string concatenation in SQL
        if (SQL_INJECTION_PATTERN.matcher(code).find()) {
            int line = findLineNumber(code, SQL_INJECTION_PATTERN);
            issues.add(CodeIssue.builder()
                    .reviewId(reviewId)
                    .severity(CodeIssue.Severity.CRITICAL)
                    .category("security")
                    .title("SQL Injection vulnerability")
                    .description("SQL query constructed with string concatenation, allowing potential SQL injection")
                    .lineNumber(line)
                    .suggestion("Use parameterized queries or prepared statements instead of string concatenation")
                    .ruleId(getName())
                    .agentType("STATIC_ANALYSIS")
                    .toolName("RuleEngine")
                    .build());
        }

        // Check for dangerous API calls with string concatenation
        if (DANGEROUS_SQL_PATTERN.matcher(code).find()) {
            int line = findLineNumber(code, DANGEROUS_SQL_PATTERN);
            issues.add(CodeIssue.builder()
                    .reviewId(reviewId)
                    .severity(CodeIssue.Severity.HIGH)
                    .category("security")
                    .title("Unsafe SQL execution")
                    .description("SQL execution with potentially unsafe input (string concatenation)")
                    .lineNumber(line)
                    .suggestion("Validate and sanitize input, use prepared statements or parameterized queries")
                    .ruleId("UNSAFE_SQL")
                    .agentType("STATIC_ANALYSIS")
                    .toolName("RuleEngine")
                    .build());
        }

        // Check for string formatting in SQL
        if (FORMAT_SQL_PATTERN.matcher(code).find()) {
            int line = findLineNumber(code, FORMAT_SQL_PATTERN);
            issues.add(CodeIssue.builder()
                    .reviewId(reviewId)
                    .severity(CodeIssue.Severity.HIGH)
                    .category("security")
                    .title("SQL with string formatting")
                    .description("SQL query using string formatting, vulnerable to SQL injection")
                    .lineNumber(line)
                    .suggestion("Use parameterized queries instead of string formatting")
                    .ruleId("FORMAT_SQL")
                    .agentType("STATIC_ANALYSIS")
                    .toolName("RuleEngine")
                    .build());
        }

        return issues;
    }
}
