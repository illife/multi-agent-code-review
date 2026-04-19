package com.codereview.ai.domain.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Agent Performance Metrics (using Micrometer)
 *
 * Collects and reports metrics for agent execution, AI API calls, and code reviews.
 * All metrics are automatically exported to Prometheus for monitoring and analysis.
 *
 * @author Code Review AI Team
 */
@Component
@RequiredArgsConstructor
public class AgentMetrics {

    private final MeterRegistry meterRegistry;

    // Issue count gauges for tracking real-time values
    private final AtomicInteger currentIssueCount = new AtomicInteger(0);

    /**
     * Record Agent execution time and statistics
     *
     * @param agentName The name of the agent that executed
     * @param durationMs The execution time in milliseconds
     * @param issuesFound The number of issues found by the agent
     */
    public void recordAgentExecution(String agentName, long durationMs, int issuesFound) {
        // Timer: records execution time distribution
        Timer.builder("agent.execution.duration")
            .tag("agent", agentName)
            .description("Agent execution duration in milliseconds")
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);

        // Counter: records total execution count
        Counter.builder("agent.execution.count")
            .tag("agent", agentName)
            .description("Total number of agent executions")
            .register(meterRegistry)
            .increment();

        // Update the issue count gauge
        currentIssueCount.set(issuesFound);
        Gauge.builder("agent.issues.found", currentIssueCount, AtomicInteger::get)
            .tag("agent", agentName)
            .description("Number of issues found by agent")
            .register(meterRegistry);
    }

    /**
     * Record AI API call metrics
     *
     * @param durationMs The API call duration in milliseconds
     * @param tokens The number of tokens consumed (if available)
     * @param success Whether the API call was successful
     */
    public void recordAICall(long durationMs, long tokens, boolean success) {
        // Timer: records AI API call duration
        Timer.builder("ai.api.duration")
            .tag("success", String.valueOf(success))
            .description("AI API call duration")
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);

        // Counter: records total tokens consumed
        if (tokens > 0) {
            Counter.builder("ai.api.tokens")
                .description("Total AI API tokens consumed")
                .register(meterRegistry)
                .increment(tokens);
        }

        // Counter: records API errors
        if (!success) {
            Counter.builder("ai.api.errors")
                .description("AI API error count")
                .register(meterRegistry)
                .increment();
        }
    }

    /**
     * Record code review statistics
     *
     * @param language The programming language being reviewed
     * @param hasIssues Whether the review found any issues
     */
    public void recordReview(String language, boolean hasIssues) {
        // Counter: records total reviews
        Counter.builder("review.total")
            .tag("language", language)
            .description("Total code reviews")
            .register(meterRegistry)
            .increment();

        // Counter: records reviews that found issues
        if (hasIssues) {
            Counter.builder("review.with_issues")
                .tag("language", language)
                .description("Reviews that found issues")
                .register(meterRegistry)
                .increment();
        }
    }

    /**
     * Record tool execution metrics
     *
     * @param toolName The name of the tool that was executed
     * @param durationMs The execution time in milliseconds
     * @param success Whether the tool execution was successful
     */
    public void recordToolExecution(String toolName, long durationMs, boolean success) {
        Timer.builder("tool.execution.duration")
            .tag("tool", toolName)
            .tag("success", String.valueOf(success))
            .description("Tool execution duration")
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);

        Counter.builder("tool.execution.count")
            .tag("tool", toolName)
            .tag("success", String.valueOf(success))
            .description("Total tool executions")
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record orchestrator parallel execution metrics
     *
     * @param agentCount The number of agents that ran in parallel
     * @param totalDurationMs The total duration for all agents to complete
     */
    public void recordOrchestratorExecution(int agentCount, long totalDurationMs) {
        Timer.builder("orchestrator.execution.duration")
            .description("Total orchestrator execution duration")
            .register(meterRegistry)
            .record(totalDurationMs, TimeUnit.MILLISECONDS);

        Gauge.builder("orchestrator.agents.parallel", () -> agentCount)
            .description("Number of agents running in parallel")
            .register(meterRegistry);
    }

    /**
     * Record cache hit/miss metrics
     *
     * @param language The programming language being reviewed
     */
    public void recordCacheHit(String language) {
        Counter.builder("cache.hit")
            .tag("language", language)
            .description("Cache hit count")
            .register(meterRegistry)
            .increment();
    }

    /**
     * Record cache miss
     *
     * @param language The programming language being reviewed
     */
    public void recordCacheMiss(String language) {
        Counter.builder("cache.miss")
            .tag("language", language)
            .description("Cache miss count")
            .register(meterRegistry)
            .increment();
    }

    /**
     * Increment a custom counter for tracking specific events
     *
     * @param name The name of the counter
     * @param tags Tags to associate with the counter
     */
    public void incrementCounter(String name, String... tags) {
        Counter.Builder builder = Counter.builder(name);
        for (int i = 0; i < tags.length; i += 2) {
            if (i + 1 < tags.length) {
                builder.tag(tags[i], tags[i + 1]);
            }
        }
        builder.register(meterRegistry).increment();
    }
}
