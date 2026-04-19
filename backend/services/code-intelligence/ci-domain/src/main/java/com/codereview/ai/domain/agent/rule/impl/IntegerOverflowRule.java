package com.codereview.ai.domain.agent.rule.impl;

import com.codereview.ai.domain.agent.rule.ReviewRule;
import com.codereview.ai.domain.model.CodeIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Integer Overflow Detection Rule
 *
 * Detects potential integer overflow vulnerabilities.
 *
 * @author Code Review AI Team
 */
@Slf4j
@Component
public class IntegerOverflowRule implements ReviewRule {

    // Pattern: Array allocation with user input
    private static final Pattern ARRAY_ALLOCATION_PATTERN = Pattern.compile(
        "new\\s+(int|integer|long|byte|short)\\s*\\[\\s*[^\\]]+\\s*[+-]\\s*[^\\]]*\\s*\\]",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern: Unsafe multiplication or addition
    private static final Pattern UNSAFE_MATH_PATTERN = Pattern.compile(
        "(size|length|count|capacity)\\s*\\*\\s*[^;]+;\\s*new",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern: Casting without overflow check
    private static final Pattern UNSAFE_CAST_PATTERN = Pattern.compile(
        "\\(\\s*(int|integer|long|byte|short)\\s*\\)\\s*[^;]+\\s*[+-]{2}",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public String getName() {
        return "INTEGER_OVERFLOW";
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
        return List.of("java", "c", "c++", "c#", "go", "kotlin", "javascript", "typescript")
                .contains(language.toLowerCase());
    }

    @Override
    public List<CodeIssue> check(String code, Long reviewId) {
        List<CodeIssue> issues = new ArrayList<>();

        if (ARRAY_ALLOCATION_PATTERN.matcher(code).find()) {
            int line = findLineNumber(code, ARRAY_ALLOCATION_PATTERN);
            issues.add(CodeIssue.builder()
                    .reviewId(reviewId)
                    .severity(CodeIssue.Severity.MEDIUM)
                    .category("bug")
                    .title("Potential integer overflow in array allocation")
                    .description("Array size calculation may overflow, leading to negative allocation")
                    .lineNumber(line)
                    .suggestion("Validate array size calculations and use safe multiplication methods")
                    .ruleId(getName())
                    .agentType("STATIC_ANALYSIS")
                    .toolName("RuleEngine")
                    .build());
        }

        if (UNSAFE_MATH_PATTERN.matcher(code).find()) {
            int line = findLineNumber(code, UNSAFE_MATH_PATTERN);
            issues.add(CodeIssue.builder()
                    .reviewId(reviewId)
                    .severity(CodeIssue.Severity.LOW)
                    .category("bug")
                    .title("Potential integer overflow")
                    .description("Math operation may overflow without bounds checking")
                    .lineNumber(line)
                    .suggestion("Use Math.multiplyExact() or add overflow checks")
                    .ruleId("UNSAFE_MATH")
                    .agentType("STATIC_ANALYSIS")
                    .toolName("RuleEngine")
                    .build());
        }

        return issues;
    }
}
