package com.codereview.ai.domain.agent.rule;

import com.codereview.ai.domain.model.CodeIssue;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Review Rule Interface
 *
 * All code review rules must implement this interface.
 * Rules detect specific code patterns and issues.
 *
 * @author Code Review AI Team
 */
public interface ReviewRule {

    /**
     * Get the unique name of this rule
     */
    String getName();

    /**
     * Get the category of issues this rule detects
     */
    String getCategory();

    /**
     * Get the severity of issues detected by this rule
     */
    CodeIssue.Severity getSeverity();

    /**
     * Check if this rule supports the given language
     *
     * @param language The programming language
     * @return true if the rule can analyze code in this language
     */
    boolean supportsLanguage(String language);

    /**
     * Check code for issues and return a list of detected problems
     *
     * @param code The code to check
     * @param reviewId The ID of the code review
     * @return List of detected issues
     */
    List<CodeIssue> check(String code, Long reviewId);

    /**
     * Helper method to find the line number of a pattern in code
     *
     * @param code The code to search
     * @param pattern The pattern to search for
     * @return The line number (1-based) or -1 if not found
     */
    default int findLineNumber(String code, Pattern pattern) {
        String[] lines = code.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (pattern.matcher(lines[i]).find()) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * Helper method to find all line numbers of a pattern in code
     *
     * @param code The code to search
     * @param pattern The pattern to search for
     * @return List of line numbers (1-based) where the pattern was found
     */
    default List<Integer> findAllLineNumbers(String code, Pattern pattern) {
        List<Integer> lineNumbers = new java.util.ArrayList<>();
        String[] lines = code.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (pattern.matcher(lines[i]).find()) {
                lineNumbers.add(i + 1);
            }
        }
        return lineNumbers;
    }

    /**
     * Helper method to extract a code snippet around a line
     *
     * @param code The full code
     * @param lineNumber The line number (1-based)
     * @param contextLines Number of lines before and after to include
     * @return The code snippet
     */
    default String extractSnippet(String code, int lineNumber, int contextLines) {
        String[] lines = code.split("\n");
        if (lineNumber < 1 || lineNumber > lines.length) {
            return "";
        }

        int start = Math.max(0, lineNumber - contextLines - 1);
        int end = Math.min(lines.length, lineNumber + contextLines);

        StringBuilder snippet = new StringBuilder();
        for (int i = start; i < end; i++) {
            snippet.append(lines[i]);
            if (i < end - 1) {
                snippet.append("\n");
            }
        }

        return snippet.toString();
    }
}
