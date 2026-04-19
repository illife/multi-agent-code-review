package com.codereview.ai.domain.agent.rule.impl;

import com.codereview.ai.domain.agent.rule.ReviewRule;
import com.codereview.ai.domain.model.CodeIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Concurrent Modification Detection Rule
 *
 * Detects potential concurrent modification exceptions.
 *
 * @author Code Review AI Team
 */
@Slf4j
@Component
public class ConcurrentModificationRule implements ReviewRule {

    // Pattern: Modifying collection while iterating
    private static final Pattern MODIFY_DURING_ITERATION_PATTERN = Pattern.compile(
        "for\\s*\\([^)]*\\)[^{]*\\{[^}]*\\.(add|remove|clear)\\s*\\(",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Pattern: Iterator remove vs collection remove
    private static final Pattern ITERATOR_REMOVE_PATTERN = Pattern.compile(
        "Iterator\\s+\\w+\\s*=\\s*\\w+\\.iterator\\s*\\(\\s*\\)[^;]*;[^}]*\\w+\\.remove",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Pattern: Stream with modification
    private static final Pattern STREAM_MODIFY_PATTERN = Pattern.compile(
        "\\.stream\\s*\\(\\s*\\)[^;]*\\.filter[^;]*;[^}]*\\.(add|remove)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    @Override
    public String getName() {
        return "CONCURRENT_MODIFICATION";
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
        return List.of("java", "javascript", "typescript", "python", "c#", "kotlin")
                .contains(language.toLowerCase());
    }

    @Override
    public List<CodeIssue> check(String code, Long reviewId) {
        List<CodeIssue> issues = new ArrayList<>();

        if (MODIFY_DURING_ITERATION_PATTERN.matcher(code).find()) {
            int line = findLineNumber(code, MODIFY_DURING_ITERATION_PATTERN);
            issues.add(CodeIssue.builder()
                    .reviewId(reviewId)
                    .severity(CodeIssue.Severity.MEDIUM)
                    .category("bug")
                    .title("Potential concurrent modification")
                    .description("Collection is modified while being iterated")
                    .lineNumber(line)
                    .suggestion("Use Iterator.remove() or collect elements to remove first, then remove them after iteration")
                    .ruleId(getName())
                    .agentType("STATIC_ANALYSIS")
                    .toolName("RuleEngine")
                    .build());
        }

        return issues;
    }
}
