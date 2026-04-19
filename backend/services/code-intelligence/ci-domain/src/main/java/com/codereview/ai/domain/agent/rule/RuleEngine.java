package com.codereview.ai.domain.agent.rule;

import com.codereview.ai.domain.agent.rule.impl.*;
import com.codereview.ai.domain.model.CodeIssue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule Engine
 *
 * Manages and applies all code review rules.
 * Automatically loads all available rule implementations.
 *
 * @author Code Review AI Team
 */
@Slf4j
@Component
public class RuleEngine {

    private final List<ReviewRule> rules;

    public RuleEngine() {
        this.rules = loadRules();
        log.info("RuleEngine initialized with {} rules", rules.size());
        for (ReviewRule rule : rules) {
            log.debug("  - {} ({}, {})", rule.getName(), rule.getCategory(), rule.getSeverity());
        }
    }

    /**
     * Apply all applicable rules to the code and return detected issues
     *
     * @param code The code to analyze
     * @param language The programming language
     * @param reviewId The ID of the code review
     * @return List of detected issues
     */
    public List<CodeIssue> applyRules(String code, String language, Long reviewId) {
        List<CodeIssue> issues = new ArrayList<>();

        for (ReviewRule rule : rules) {
            if (rule.supportsLanguage(language)) {
                try {
                    List<CodeIssue> ruleIssues = rule.check(code, reviewId);
                    if (!ruleIssues.isEmpty()) {
                        log.debug("Rule {} found {} issues", rule.getName(), ruleIssues.size());
                        issues.addAll(ruleIssues);
                    }
                } catch (Exception e) {
                    log.error("Error executing rule: {}", rule.getName(), e);
                }
            }
        }

        return issues;
    }

    /**
     * Get all registered rules
     */
    public List<ReviewRule> getAllRules() {
        return new ArrayList<>(rules);
    }

    /**
     * Get rules by category
     */
    public List<ReviewRule> getRulesByCategory(String category) {
        return rules.stream()
                .filter(rule -> rule.getCategory().equalsIgnoreCase(category))
                .toList();
    }

    /**
     * Load all rule implementations
     */
    private List<ReviewRule> loadRules() {
        return List.of(
            // Security rules
            new SqlInjectionRule(),
            new HardcodedSecretRule(),
            new XssVulnerabilityRule(),
            new UnvalidatedRedirectRule(),
            new WeakCryptoRule(),

            // Bug detection rules
            new ResourceLeakRule(),
            new NullPointerRule(),
            new ConcurrentModificationRule(),
            new IntegerOverflowRule(),
            new FormatStringRule()
        );
    }
}
