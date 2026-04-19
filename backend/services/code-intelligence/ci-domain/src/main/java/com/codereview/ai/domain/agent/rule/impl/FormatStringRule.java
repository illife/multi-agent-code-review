package com.codereview.ai.domain.agent.rule.impl;

import com.codereview.ai.domain.agent.rule.ReviewRule;
import com.codereview.ai.domain.model.CodeIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Format String Vulnerability Detection Rule
 *
 * Detects potential format string vulnerabilities.
 *
 * @author Code Review AI Team
 */
@Slf4j
@Component
public class FormatStringRule implements ReviewRule {

    // Pattern: Using user input as format string
    private static final Pattern FORMAT_STRING_PATTERN = Pattern.compile(
        "(printf|sprintf|fprintf|snprintf|String\\.format|System\\.out\\.printf|Logger\\.info|Logger\\.debug)\\s*\\([^,]*,[^)]*\\+[^)]*\\)",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern: Format string with user input
    private static final Pattern USER_INPUT_FORMAT_PATTERN = Pattern.compile(
        "(printf|sprintf|format)\\s*\\(\\s*[^,]+,\\s*[^)]+\\+[^)]*\\s*\\)",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern: Log with user input as format
    private static final Pattern LOG_FORMAT_PATTERN = Pattern.compile(
        "(log|logger|Logger)\\.[a-z]+\\s*\\([^,]*,[^,]*\\+",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public String getName() {
        return "FORMAT_STRING";
    }

    @Override
    public String getCategory() {
        return "security";
    }

    @Override
    public CodeIssue.Severity getSeverity() {
        return CodeIssue.Severity.MEDIUM;
    }

    @Override
    public boolean supportsLanguage(String language) {
        return List.of("c", "c++", "java", "python", "javascript", "typescript", "c#", "php", "ruby")
                .contains(language.toLowerCase());
    }

    @Override
    public List<CodeIssue> check(String code, Long reviewId) {
        List<CodeIssue> issues = new ArrayList<>();

        if (FORMAT_STRING_PATTERN.matcher(code).find()) {
            int line = findLineNumber(code, FORMAT_STRING_PATTERN);
            issues.add(CodeIssue.builder()
                    .reviewId(reviewId)
                    .severity(CodeIssue.Severity.MEDIUM)
                    .category("security")
                    .title("Potential format string vulnerability")
                    .description("User input may be used as format string, allowing format string attacks")
                    .lineNumber(line)
                    .suggestion("Use constant format strings and pass user input as arguments, not as the format string itself")
                    .ruleId(getName())
                    .agentType("STATIC_ANALYSIS")
                    .toolName("RuleEngine")
                    .build());
        }

        if (LOG_FORMAT_PATTERN.matcher(code).find()) {
            int line = findLineNumber(code, LOG_FORMAT_PATTERN);
            issues.add(CodeIssue.builder()
                    .reviewId(reviewId)
                    .severity(CodeIssue.Severity.LOW)
                    .category("best-practice")
                    .title("Unsafe log formatting")
                    .description("User input may be concatenated into log message, potentially causing format issues")
                    .lineNumber(line)
                    .suggestion("Use parameterized logging with {} placeholders")
                    .ruleId("LOG_FORMAT")
                    .agentType("STATIC_ANALYSIS")
                    .toolName("RuleEngine")
                    .build());
        }

        return issues;
    }
}
