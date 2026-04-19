package com.codereview.ai.domain.service.impl;

import com.codereview.ai.domain.agent.shared.AgentExecutionContext;
import com.codereview.ai.domain.agent.shared.AgentExecutionResult;
import com.codereview.ai.domain.agent.shared.AgentOrchestrationService;
import com.codereview.ai.domain.model.CodeIssue;
import com.codereview.ai.domain.model.CodeReview;
import com.codereview.ai.domain.model.TeachingReport;
import com.codereview.ai.domain.repository.CodeIssueRepository;
import com.codereview.ai.domain.repository.CodeReviewRepository;
import com.codereview.ai.domain.repository.TeachingReportRepository;
import com.codereview.ai.domain.service.CodeReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Code Review Service Implementation
 *
 * @author Code Review AI Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeReviewServiceImpl implements CodeReviewService {

    private final CodeReviewRepository reviewRepository;
    private final CodeIssueRepository issueRepository;
    private final TeachingReportRepository teachingReportRepository;
    private final AgentOrchestrationService agentOrchestrationService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topic.code-review:code-review-processing}")
    private String codeReviewTopic;

    @Override
    @Transactional
    public ReviewResultDTO submitReview(ReviewRequestDTO request) throws Exception {
        CodeReview review = CodeReview.builder()
                .userId(request.getUserId())
                .codeContent(request.getCode())
                .language(request.getLanguage())
                .fileName(request.getFileName())
                .visibility(request.getVisibility() != null ? request.getVisibility() : "PRIVATE")
                .status(CodeReview.ReviewStatus.PENDING)
                .build();

        review = reviewRepository.save(review);
        log.info("Created code review record: reviewId={}, userId={}", review.getId(), request.getUserId());

        review.setStatus(CodeReview.ReviewStatus.PROCESSING);
        reviewRepository.save(review);

        try {
            // Build execution context
            AgentExecutionContext context = AgentExecutionContext.builder()
                    .requestId("review-" + review.getId())
                    .userId(review.getUserId())
                    .code(review.getCodeContent())
                    .language(review.getLanguage())
                    .filePath(review.getFileName())
                    .build();

            // Execute code review agents
            List<AgentExecutionResult> results = agentOrchestrationService.executeCodeReview(context);

            // Convert results to CodeIssue entities and save
            List<CodeIssue> allIssues = new java.util.ArrayList<>();
            for (AgentExecutionResult result : results) {
                if (result.isSuccess() && result.getIssues() != null) {
                    for (AgentExecutionResult.AgentIssue agentIssue : result.getIssues()) {
                        CodeIssue issue = convertAgentIssueToCodeIssue(agentIssue, review);
                        allIssues.add(issue);
                    }
                }
            }
            issueRepository.saveAll(allIssues);

            review.setStatus(CodeReview.ReviewStatus.COMPLETED);
            review.setTotalIssues(allIssues.size());
            reviewRepository.save(review);

            log.info("Code review completed: reviewId={}, totalIssues={}", review.getId(), allIssues.size());

            return ReviewResultDTO.builder()
                    .reviewId(review.getId())
                    .status("COMPLETED")
                    .totalIssues(allIssues.size())
                    .issues(allIssues)
                    .build();

        } catch (Exception e) {
            log.error("Code review failed: reviewId={}", review.getId(), e);
            review.setStatus(CodeReview.ReviewStatus.FAILED);
            reviewRepository.save(review);
            throw e;
        }
    }

    @Override
    @Transactional
    public Long submitReviewAsync(ReviewRequestDTO request) {
        CodeReview review = CodeReview.builder()
                .userId(request.getUserId())
                .codeContent(request.getCode())
                .language(request.getLanguage())
                .fileName(request.getFileName())
                .visibility(request.getVisibility() != null ? request.getVisibility() : "PRIVATE")
                .status(CodeReview.ReviewStatus.PENDING)
                .build();

        review = reviewRepository.save(review);
        log.info("Created async code review record: reviewId={}, userId={}", review.getId(), request.getUserId());

        kafkaTemplate.send(codeReviewTopic, review.getId().toString());
        log.info("Sent Kafka message: topic={}, reviewId={}", codeReviewTopic, review.getId());

        return review.getId();
    }

    @Override
    public CodeReview getReview(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
    }

    @Override
    public ReviewDetailDTO getReviewDetail(Long reviewId) {
        CodeReview review = getReview(reviewId);
        List<CodeIssue> issues = issueRepository.findByReviewIdOrderBySeverity(reviewId);

        ReviewDetailDTO detail = new ReviewDetailDTO();
        detail.setReviewId(review.getId());
        detail.setCodeContent(review.getCodeContent());
        detail.setLanguage(review.getLanguage());
        detail.setFileName(review.getFileName());
        detail.setStatus(review.getStatus().name());
        detail.setTotalIssues(review.getTotalIssues());
        detail.setIssues(issues);

        // Load teaching report if exists
        teachingReportRepository.findByReviewId(reviewId).ifPresent(detail::setTeachingReport);

        return detail;
    }

    @Override
    public Page<CodeReview> getUserReviews(Long userId, Pageable pageable) {
        return reviewRepository.findByUserId(userId, pageable);
    }

    @Override
    public Page<CodeIssue> getReviewIssues(Long reviewId, Pageable pageable) {
        return issueRepository.findByReviewId(reviewId, pageable);
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        CodeReview review = getReview(reviewId);

        if (!review.getUserId().equals(userId)) {
            throw new IllegalArgumentException("No permission to delete this review");
        }

        // Delete teaching report if exists
        teachingReportRepository.deleteByReviewId(reviewId);

        issueRepository.deleteByReviewId(reviewId);
        reviewRepository.delete(review);

        log.info("Deleted code review record: reviewId={}", reviewId);
    }

    /**
     * Convert AgentIssue to CodeIssue entity
     */
    private CodeIssue convertAgentIssueToCodeIssue(AgentExecutionResult.AgentIssue agentIssue, CodeReview review) {
        return CodeIssue.builder()
                .reviewId(review.getId())
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

    @Override
    public java.util.List<AgentExecutionDTO> getAgentExecutions(Long reviewId) {
        CodeReview review = getReview(reviewId);

        // Define all four code review agents
        java.util.List<AgentExecutionDTO> agents = new java.util.ArrayList<>();

        // Map of agent type to display name
        java.util.Map<String, String> agentNames = new java.util.LinkedHashMap<>();
        agentNames.put("CODE_STANDARDS_INSPECTOR", "代码规范检查员");
        agentNames.put("ARCHITECTURE_GUARDIAN", "架构守护者");
        agentNames.put("SECURITY_AUDITOR", "安全审计员");
        agentNames.put("PERFORMANCE_OPTIMIZER", "性能优化师");

        // Create agent DTOs
        for (java.util.Map.Entry<String, String> entry : agentNames.entrySet()) {
            String agentType = entry.getKey();
            String agentName = entry.getValue();

            String status = switch (review.getStatus()) {
                case COMPLETED -> "COMPLETED";
                case PROCESSING -> "RUNNING";
                case FAILED -> "FAILED";
                default -> "PENDING";
            };

            agents.add(new AgentExecutionDTO(agentType, agentName, status, 0));
        }

        // If review is completed, count issues by agent type from database
        if (review.getStatus() == CodeReview.ReviewStatus.COMPLETED) {
            java.util.List<CodeIssue> issues = issueRepository.findByReviewIdOrderBySeverity(reviewId);
            java.util.Map<String, Long> issuesByAgent = issues.stream()
                    .filter(issue -> issue.getAgentType() != null)
                    .collect(java.util.stream.Collectors.groupingBy(
                            CodeIssue::getAgentType,
                            java.util.stream.Collectors.counting()
                    ));

            // Update issue counts for all agents
            for (AgentExecutionDTO agent : agents) {
                Long count = issuesByAgent.getOrDefault(agent.getAgentType(), 0L);
                agent.setIssuesFound(count.intValue());
            }
        }

        return agents;
    }

    @Override
    public String getTeachingReportMarkdown(Long reviewId) {
        TeachingReport report = teachingReportRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Teaching report not found for review: " + reviewId));

        StringBuilder markdown = new StringBuilder();
        markdown.append("# 教学报告\n\n");
        markdown.append("**审查ID**: ").append(reviewId).append("\n\n");
        markdown.append("**编程语言**: ").append(report.getLanguage()).append("\n\n");
        markdown.append("---\n\n");

        // Summary
        if (report.getSummary() != null && !report.getSummary().isEmpty()) {
            markdown.append("## 总结\n\n");
            markdown.append(report.getSummary()).append("\n\n");
        }

        // Code Summary
        if (report.getCodeSummary() != null && !report.getCodeSummary().isEmpty()) {
            markdown.append("## 代码概述\n\n");
            markdown.append(report.getCodeSummary()).append("\n\n");
        }

        // Issue Statistics
        markdown.append("## 问题统计\n\n");
        markdown.append("- 总计: ").append(report.getTotalIssues()).append(" 个问题\n");
        markdown.append("- 严重: ").append(report.getCriticalIssues()).append(" 个\n");
        markdown.append("- 高危: ").append(report.getHighIssues()).append(" 个\n");
        markdown.append("- 中等: ").append(report.getMediumIssues()).append(" 个\n");
        markdown.append("- 低危: ").append(report.getLowIssues()).append(" 个\n\n");

        // Knowledge Gaps
        if (!report.getKnowledgeGaps().isEmpty()) {
            markdown.append("## 知识缺口\n\n");
            for (String gap : report.getKnowledgeGaps()) {
                markdown.append("- ").append(gap).append("\n");
            }
            markdown.append("\n");
        }

        // Key Findings
        if (!report.getKeyFindings().isEmpty()) {
            markdown.append("## 关键发现\n\n");
            for (TeachingReport.KeyFinding finding : report.getKeyFindings()) {
                markdown.append("### ").append(finding.getCategory()).append("\n\n");
                markdown.append("**问题**: ").append(finding.getIssue()).append("\n\n");
                markdown.append("**严重程度**: ").append(finding.getSeverity()).append("\n\n");
                if (finding.getExplanation() != null && !finding.getExplanation().isEmpty()) {
                    markdown.append("**解释**: ").append(finding.getExplanation()).append("\n\n");
                }
                if (finding.getImprovement() != null && !finding.getImprovement().isEmpty()) {
                    markdown.append("**改进建议**: ").append(finding.getImprovement()).append("\n\n");
                }
            }
        }

        // Learning Resources
        if (!report.getLearningResources().isEmpty()) {
            markdown.append("## 学习资源\n\n");
            for (java.util.Map.Entry<String, List<String>> entry : report.getLearningResources().entrySet()) {
                markdown.append("### ").append(entry.getKey()).append("\n\n");
                for (String resource : entry.getValue()) {
                    markdown.append("- ").append(resource).append("\n");
                }
                markdown.append("\n");
            }
        }

        // Priority Actions
        if (!report.getPriorityActions().isEmpty()) {
            markdown.append("## 优先行动项\n\n");
            for (int i = 0; i < report.getPriorityActions().size(); i++) {
                markdown.append((i + 1)).append(". ").append(report.getPriorityActions().get(i)).append("\n");
            }
            markdown.append("\n");
        }

        // Encouragement
        if (report.getEncouragement() != null && !report.getEncouragement().isEmpty()) {
            markdown.append("---\n\n");
            markdown.append("**鼓励**: ").append(report.getEncouragement()).append("\n\n");
        }

        return markdown.toString();
    }

    @Override
    public String getFullReportMarkdown(Long reviewId) {
        ReviewDetailDTO detail = getReviewDetail(reviewId);
        StringBuilder markdown = new StringBuilder();

        markdown.append("# 代码审查报告\n\n");
        markdown.append("**审查ID**: ").append(reviewId).append("\n");
        markdown.append("**文件名**: ").append(detail.getFileName()).append("\n");
        markdown.append("**编程语言**: ").append(detail.getLanguage()).append("\n");
        markdown.append("**状态**: ").append(detail.getStatus()).append("\n");
        markdown.append("**问题总数**: ").append(detail.getTotalIssues()).append("\n\n");
        markdown.append("---\n\n");

        // Teaching Report Section
        if (detail.getTeachingReport() != null) {
            markdown.append(getTeachingReportMarkdown(reviewId));
            markdown.append("\n\n---\n\n");
        }

        // Agent Executions
        markdown.append("## 智能体执行详情\n\n");
        java.util.List<AgentExecutionDTO> agents = getAgentExecutions(reviewId);
        for (AgentExecutionDTO agent : agents) {
            markdown.append("### ").append(agent.getAgentName()).append("\n\n");
            markdown.append("- **类型**: ").append(agent.getAgentType()).append("\n");
            markdown.append("- **状态**: ").append(agent.getStatus()).append("\n");
            markdown.append("- **发现问题数**: ").append(agent.getIssuesFound()).append("\n\n");
        }

        // Issues List
        if (detail.getIssues() != null && !detail.getIssues().isEmpty()) {
            markdown.append("## 问题列表\n\n");
            for (CodeIssue issue : detail.getIssues()) {
                markdown.append("### ").append(issue.getTitle()).append("\n\n");
                markdown.append("- **严重程度**: ").append(issue.getSeverity()).append("\n");
                markdown.append("- **类别**: ").append(issue.getCategory()).append("\n");
                if (issue.getLineNumber() != null) {
                    markdown.append("- **行号**: ").append(issue.getLineNumber()).append("\n");
                }
                markdown.append("\n");

                if (issue.getDescription() != null && !issue.getDescription().isEmpty()) {
                    markdown.append("**描述**: ").append(issue.getDescription()).append("\n\n");
                }

                if (issue.getCodeSnippet() != null && !issue.getCodeSnippet().isEmpty()) {
                    markdown.append("**代码片段**:\n```\n").append(issue.getCodeSnippet()).append("\n```\n\n");
                }

                if (issue.getSuggestion() != null && !issue.getSuggestion().isEmpty()) {
                    markdown.append("**建议**: ").append(issue.getSuggestion()).append("\n\n");
                }

                if (issue.getTeachingExplanation() != null && !issue.getTeachingExplanation().isEmpty()) {
                    markdown.append("**教学解释**: ").append(issue.getTeachingExplanation()).append("\n\n");
                }

                markdown.append("---\n\n");
            }
        }

        return markdown.toString();
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
}
