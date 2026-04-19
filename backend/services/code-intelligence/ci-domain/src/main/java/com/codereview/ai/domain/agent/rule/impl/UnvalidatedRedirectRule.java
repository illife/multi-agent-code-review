package com.codereview.ai.domain.agent.rule.impl;

import com.codereview.ai.domain.agent.rule.ReviewRule;
import com.codereview.ai.domain.model.CodeIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Unvalidated Redirect Detection Rule
 *
 * Detects potential open redirect vulnerabilities.
 *
 * @author Code Review AI Team
 */
@Slf4j
@Component
public class UnvalidatedRedirectRule implements ReviewRule {

    // Pattern: Redirect using user input without validation
    private static final Pattern REDIRECT_PATTERN = Pattern.compile(
        "(redirect|sendRedirect|response\\.redirect|header\\s*\\(\\s*[\"']Location)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern USER_INPUT_REDIRECT_PATTERN = Pattern.compile(
        "(redirect|sendRedirect|response\\.redirect)\\s*\\([^)]*\\+[^)]*\\)",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public String getName() {
        return "UNVALIDATED_REDIRECT";
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
        return List.of("java", "javascript", "typescript", "python", "php", "go", "c#", "ruby")
                .contains(language.toLowerCase());
    }

    @Override
    public List<CodeIssue> check(String code, Long reviewId) {
        List<CodeIssue> issues = new ArrayList<>();

        if (USER_INPUT_REDIRECT_PATTERN.matcher(code).find()) {
            int line = findLineNumber(code, USER_INPUT_REDIRECT_PATTERN);
            issues.add(CodeIssue.builder()
                    .reviewId(reviewId)
                    .severity(CodeIssue.Severity.HIGH)
                    .category("security")
                    .title("Unvalidated redirect")
                    .description("Redirect using user input without proper validation")
                    .lineNumber(line)
                    .suggestion("Validate and whitelist redirect URLs, don't use user input directly in redirects")
                    .ruleId(getName())
                    .agentType("STATIC_ANALYSIS")
                    .toolName("RuleEngine")
                    .build());
        }

        return issues;
    }
}
