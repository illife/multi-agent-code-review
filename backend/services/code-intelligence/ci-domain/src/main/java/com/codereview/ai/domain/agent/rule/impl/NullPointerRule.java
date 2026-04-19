package com.codereview.ai.domain.agent.rule.impl;

import com.codereview.ai.domain.agent.rule.ReviewRule;
import com.codereview.ai.domain.model.CodeIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Null Pointer Dereference Detection Rule
 *
 * Detects potential null pointer exceptions.
 *
 * @author Code Review AI Team
 */
@Slf4j
@Component
public class NullPointerRule implements ReviewRule {

    // Pattern: Dereferencing without null check
    private static final Pattern DEREFERENCE_PATTERN = Pattern.compile(
        "\\b[a-zA-Z_]\\w*\\.(get|set|call|invoke|run|execute|process|handle)\\s*\\(",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern: Chained method calls without null checks
    private static final Pattern CHAINED_CALL_PATTERN = Pattern.compile(
        "[a-zA-Z_]\\w*\\.\\w+\\.[a-zA-Z_]\\w*\\s*\\(",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern: Return value used without null check
    private static final Pattern UNCHECKED_RETURN_PATTERN = Pattern.compile(
        "(\\w+\\s*=[^;]+\\.get[^;]+;|\\w+\\s*=[^;]+\\(\\)[^;]+;)[^}]*\\1\\.",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    @Override
    public String getName() {
        return "NULL_POINTER";
    }

    @Override
    public String getCategory() {
        return "bug";
    }

    @Override
    public CodeIssue.Severity getSeverity() {
        return CodeIssue.Severity.MEDIUM;
    }

    @Override
    public boolean supportsLanguage(String language) {
        return List.of("java", "javascript", "typescript", "c#", "c++", "go", "kotlin")
                .contains(language.toLowerCase());
    }

    @Override
    public List<CodeIssue> check(String code, Long reviewId) {
        List<CodeIssue> issues = new ArrayList<>();

        if (CHAINED_CALL_PATTERN.matcher(code).find()) {
            int line = findLineNumber(code, CHAINED_CALL_PATTERN);
            issues.add(CodeIssue.builder()
                    .reviewId(reviewId)
                    .severity(CodeIssue.Severity.MEDIUM)
                    .category("bug")
                    .title("Potential null pointer exception")
                    .description("Chained method call without null checks may throw NPE")
                    .lineNumber(line)
                    .suggestion("Add null checks or use Optional to handle potential null values")
                    .ruleId(getName())
                    .agentType("STATIC_ANALYSIS")
                    .toolName("RuleEngine")
                    .build());
        }

        return issues;
    }
}
