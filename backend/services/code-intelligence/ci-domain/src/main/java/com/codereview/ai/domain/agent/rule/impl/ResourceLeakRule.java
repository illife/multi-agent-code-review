package com.codereview.ai.domain.agent.rule.impl;

import com.codereview.ai.domain.agent.rule.ReviewRule;
import com.codereview.ai.domain.model.CodeIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Resource Leak Detection Rule
 *
 * Detects potential resource leaks (file handles, connections, streams).
 *
 * @author Code Review AI Team
 */
@Slf4j
@Component
public class ResourceLeakRule implements ReviewRule {

    // Pattern: File/Stream creation without try-with-resources
    private static final Pattern RESOURCE_CREATION_PATTERN = Pattern.compile(
        "(new\\s+(FileInputStream|FileOutputStream|FileReader|FileWriter|BufferedReader|BufferedWriter|" +
        "InputStreamReader|OutputStreamWriter|PrintWriter|Scanner|Connection|Statement|PreparedStatement|" +
        "ResultSet|Socket|ServerSocket|DatagramSocket))",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern: Manual close in finally (prone to exceptions)
    private static final Pattern MANUAL_CLOSE_PATTERN = Pattern.compile(
        "(close\\s*\\(\\s*\\)\\s*;|finally\\s*\\{[^}]*close)",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public String getName() {
        return "RESOURCE_LEAK";
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
        return List.of("java", "python", "go", "c#", "c++", "javascript", "typescript")
                .contains(language.toLowerCase());
    }

    @Override
    public List<CodeIssue> check(String code, Long reviewId) {
        List<CodeIssue> issues = new ArrayList<>();

        boolean hasResourceCreation = RESOURCE_CREATION_PATTERN.matcher(code).find();
        boolean hasTryWithResources = code.contains("try (") || code.contains("try-with-resources");
        boolean hasManualClose = MANUAL_CLOSE_PATTERN.matcher(code).find();

        if (hasResourceCreation && !hasTryWithResources && !hasManualClose) {
            int line = findLineNumber(code, RESOURCE_CREATION_PATTERN);
            issues.add(CodeIssue.builder()
                    .reviewId(reviewId)
                    .severity(CodeIssue.Severity.MEDIUM)
                    .category("bug")
                    .title("Potential resource leak")
                    .description("Resource created without proper cleanup mechanism")
                    .lineNumber(line)
                    .suggestion("Use try-with-resources (Java), with statements (Python), or defer statements (Go)")
                    .ruleId(getName())
                    .agentType("STATIC_ANALYSIS")
                    .toolName("RuleEngine")
                    .build());
        }

        return issues;
    }
}
