package com.codereview.ai.messaging;

import com.codereview.ai.domain.agent.shared.AgentExecutionContext;
import com.codereview.ai.domain.agent.shared.AgentExecutionResult;
import com.codereview.ai.domain.agent.shared.AgentOrchestrationService;
import com.codereview.ai.domain.model.CodeIssue;
import com.codereview.ai.domain.model.CodeReview;
import com.codereview.ai.domain.model.TeachingReport;
import com.codereview.ai.domain.repository.CodeIssueRepository;
import com.codereview.ai.domain.repository.CodeReviewRepository;
import com.codereview.ai.domain.repository.TeachingReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Code Review Kafka Consumer
 *
 * Asynchronously processes code review requests
 *
 * @author Code Review AI Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeReviewConsumer {

    private final CodeReviewRepository reviewRepository;
    private final CodeIssueRepository issueRepository;
    private final TeachingReportRepository teachingReportRepository;
    private final AgentOrchestrationService agentOrchestrationService;

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("====================================");
        log.info("CodeReviewConsumer started");
        log.info("Listening topic: code-review-processing");
        log.info("Consumer group: codereview-group");
        log.info("Concurrency: 3");
        log.info("====================================");
    }

    @KafkaListener(
        topics = "code-review-processing",
        groupId = "codereview-group",
        concurrency = "3"
    )
    public void processReview(String message) {
        log.info("Received code review task: message='{}'", message);

        Long reviewId;
        try {
            reviewId = Long.parseLong(message);
            log.info("Successfully parsed reviewId: {}", reviewId);
        } catch (NumberFormatException e) {
            log.error("Unable to parse reviewId: '{}'", message, e);
            throw e;
        }

        CodeReview existingReview = reviewRepository.findById(reviewId).orElse(null);
        if (existingReview != null && existingReview.getStatus() == CodeReview.ReviewStatus.COMPLETED) {
            log.warn("Review already completed, skipping: reviewId={}, status={}",
                    reviewId, existingReview.getStatus());
            return;
        }

        try {
            CodeReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));

            if (review.getStatus() == CodeReview.ReviewStatus.COMPLETED) {
                log.warn("Review already completed, skipping: reviewId={}", reviewId);
                return;
            }

            review.setStatus(CodeReview.ReviewStatus.PROCESSING);
            reviewRepository.save(review);

            log.info("Starting agent analysis: reviewId={}, language={}", reviewId, review.getLanguage());

            // Build execution context
            AgentExecutionContext context = AgentExecutionContext.builder()
                    .requestId("review-" + reviewId)
                    .userId(review.getUserId())
                    .code(review.getCodeContent())
                    .language(review.getLanguage())
                    .filePath(review.getFileName())
                    .build();

            // Execute code review agents
            java.util.List<AgentExecutionResult> results = agentOrchestrationService.executeCodeReview(context);

            // Collect all issues and deduplicate
            java.util.List<AgentExecutionResult.AgentIssue> allIssues = new java.util.ArrayList<>();
            for (AgentExecutionResult result : results) {
                if (result.isSuccess() && result.getIssues() != null) {
                    allIssues.addAll(result.getIssues());
                }
            }

            // Deduplicate issues based on lineNumber, title, and category
            java.util.List<AgentExecutionResult.AgentIssue> uniqueIssues = deduplicateIssues(allIssues);

            // Save unique issues
            int totalIssues = 0;
            for (AgentExecutionResult.AgentIssue agentIssue : uniqueIssues) {
                CodeIssue issue = convertAgentIssueToCodeIssue(agentIssue, reviewId);
                issueRepository.save(issue);
                totalIssues++;
            }

            log.info("Saved issues to database: reviewId={}, total={}, unique={}, duplicates={}",
                    reviewId, allIssues.size(), uniqueIssues.size(), allIssues.size() - uniqueIssues.size());

            // 统计各严重级别的问题数量
            int criticalCount = (int) uniqueIssues.stream()
                    .filter(i -> i.getSeverity() == AgentExecutionResult.Severity.CRITICAL).count();
            int highCount = (int) uniqueIssues.stream()
                    .filter(i -> i.getSeverity() == AgentExecutionResult.Severity.HIGH).count();
            int mediumCount = (int) uniqueIssues.stream()
                    .filter(i -> i.getSeverity() == AgentExecutionResult.Severity.MEDIUM).count();
            int lowCount = (int) uniqueIssues.stream()
                    .filter(i -> i.getSeverity() == AgentExecutionResult.Severity.LOW).count();

            review.setStatus(CodeReview.ReviewStatus.COMPLETED);
            review.setTotalIssues(totalIssues);
            reviewRepository.save(review);

            log.info("Code review processing completed: reviewId={}, totalIssues={}", reviewId, totalIssues);

            // 生成教学报告（异步进行，不影响主流程）
            generateTeachingReportAsync(reviewId, review.getCodeContent(), review.getLanguage(),
                    review.getFileName(), review.getUserId(), totalIssues, criticalCount,
                    highCount, mediumCount, lowCount);

        } catch (Exception e) {
            log.error("====================================");
            log.error("Code review processing failed: reviewId={}", reviewId);
            log.error("Error type: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage());
            log.error("====================================");
            log.error("Full stack trace:", e);

            CodeReview review = reviewRepository.findById(reviewId).orElse(null);
            if (review != null) {
                review.setStatus(CodeReview.ReviewStatus.FAILED);
                reviewRepository.save(review);
            }

            throw new RuntimeException("Code review processing failed: reviewId=" + reviewId, e);
        }
    }

    private CodeIssue convertAgentIssueToCodeIssue(AgentExecutionResult.AgentIssue agentIssue, Long reviewId) {
        return CodeIssue.builder()
                .reviewId(reviewId)
                .title(agentIssue.getTitle())
                .description(agentIssue.getDescription())
                .severity(convertSeverity(agentIssue.getSeverity()))
                .category(agentIssue.getCategory())
                .lineNumber(agentIssue.getLineNumber())
                .codeSnippet(agentIssue.getCodeSnippet())
                .suggestion(agentIssue.getSuggestion())
                .teachingExplanation(agentIssue.getTeachingExplanation())
                .agentType(agentIssue.getAgentType())
                .build();
    }

    private CodeIssue.Severity convertSeverity(AgentExecutionResult.Severity severity) {
        return switch (severity) {
            case CRITICAL -> CodeIssue.Severity.CRITICAL;
            case HIGH -> CodeIssue.Severity.HIGH;
            case MEDIUM -> CodeIssue.Severity.MEDIUM;
            case LOW -> CodeIssue.Severity.LOW;
            case INFO -> CodeIssue.Severity.LOW;
        };
    }

    /**
     * Deduplicate issues based on lineNumber, title, category, and code snippet
     */
    private java.util.List<AgentExecutionResult.AgentIssue> deduplicateIssues(
            java.util.List<AgentExecutionResult.AgentIssue> issues) {

        // Use a set to track unique issue signatures
        java.util.Set<String> seenSignatures = new java.util.HashSet<>();
        java.util.List<AgentExecutionResult.AgentIssue> uniqueIssues = new java.util.ArrayList<>();

        for (AgentExecutionResult.AgentIssue issue : issues) {
            // Create a signature for deduplication
            String signature = createIssueSignature(issue);

            // Only add if we haven't seen this signature before
            if (!seenSignatures.contains(signature)) {
                seenSignatures.add(signature);
                uniqueIssues.add(issue);
            }
        }

        return uniqueIssues;
    }

    /**
     * Create a unique signature for an issue for deduplication
     */
    private String createIssueSignature(AgentExecutionResult.AgentIssue issue) {
        StringBuilder signature = new StringBuilder();

        // Line number is the strongest indicator of uniqueness
        if (issue.getLineNumber() != null && issue.getLineNumber() > 0) {
            signature.append("line:").append(issue.getLineNumber()).append("|");
        }

        // Title and category
        signature.append("title:").append(issue.getTitle() != null ? issue.getTitle() : "").append("|");
        signature.append("category:").append(issue.getCategory() != null ? issue.getCategory() : "").append("|");

        // Code snippet (first 50 chars) for additional uniqueness
        if (issue.getCodeSnippet() != null && !issue.getCodeSnippet().isEmpty()) {
            String truncated = issue.getCodeSnippet().length() > 50
                ? issue.getCodeSnippet().substring(0, 50)
                : issue.getCodeSnippet();
            signature.append("code:").append(truncated);
        }

        return signature.toString();
    }

    /**
     * 异步生成教学报告
     */
    private void generateTeachingReportAsync(
            Long reviewId,
            String codeContent,
            String language,
            String fileName,
            Long userId,
            int totalIssues,
            int criticalCount,
            int highCount,
            int mediumCount,
            int lowCount
    ) {
        // 使用新线程异步生成，避免阻塞主流程
        new Thread(() -> {
            try {
                log.info("Starting teaching report generation for reviewId={}", reviewId);

                TeachingReport report = agentOrchestrationService.generateTeachingReport(
                        reviewId,
                        codeContent,
                        language,
                        fileName,
                        userId,
                        totalIssues,
                        criticalCount,
                        highCount,
                        mediumCount,
                        lowCount
                );

                if (report != null) {
                    teachingReportRepository.save(report);
                    log.info("Teaching report generated successfully for reviewId={}", reviewId);
                } else {
                    log.warn("Teaching report generation returned null for reviewId={}", reviewId);
                }

            } catch (Exception e) {
                // 教学报告生成失败不应影响主流程
                log.error("Failed to generate teaching report for reviewId={} (non-critical)", reviewId, e);
            }
        }, "teaching-report-" + reviewId).start();
    }
}
