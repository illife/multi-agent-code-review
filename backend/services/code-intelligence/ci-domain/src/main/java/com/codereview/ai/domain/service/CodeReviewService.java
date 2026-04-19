package com.codereview.ai.domain.service;

import com.codereview.ai.domain.model.CodeIssue;
import com.codereview.ai.domain.model.CodeReview;
import com.codereview.ai.domain.model.TeachingReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Code Review Service Interface
 *
 * Provides core code review functionality
 *
 * @author Code Review AI Team
 */
public interface CodeReviewService {

    /**
     * Submit code review (synchronous processing)
     *
     * @param request Review request
     * @return Review result
     */
    ReviewResultDTO submitReview(ReviewRequestDTO request) throws Exception;

    /**
     * Submit code review (asynchronous processing)
     *
     * @param request Review request
     * @return Review ID
     */
    Long submitReviewAsync(ReviewRequestDTO request);

    /**
     * Get review record
     *
     * @param reviewId Review ID
     * @return Review record
     */
    CodeReview getReview(Long reviewId);

    /**
     * Get review record with issues
     *
     * @param reviewId Review ID
     * @return Review detail
     */
    ReviewDetailDTO getReviewDetail(Long reviewId);

    /**
     * Get user's review list
     *
     * @param userId User ID
     * @param pageable Pagination parameters
     * @return Review list
     */
    Page<CodeReview> getUserReviews(Long userId, Pageable pageable);

    /**
     * Get review's issue list
     *
     * @param reviewId Review ID
     * @param pageable Pagination parameters
     * @return Issue list
     */
    Page<CodeIssue> getReviewIssues(Long reviewId, Pageable pageable);

    /**
     * Delete review record
     *
     * @param reviewId Review ID
     * @param userId User ID (for permission verification)
     */
    void deleteReview(Long reviewId, Long userId);

    /**
     * Get agent execution status for a review
     * Returns a list of agent execution information
     *
     * @param reviewId Review ID
     * @return List of agent execution DTOs
     */
    java.util.List<AgentExecutionDTO> getAgentExecutions(Long reviewId);

    /**
     * Get teaching report as markdown string
     *
     * @param reviewId Review ID
     * @return Teaching report in markdown format
     */
    String getTeachingReportMarkdown(Long reviewId);

    /**
     * Get full review report as markdown string
     *
     * @param reviewId Review ID
     * @return Full review report in markdown format
     */
    String getFullReportMarkdown(Long reviewId);

    /**
     * Agent execution DTO
     */
    class AgentExecutionDTO {
        private String agentType;
        private String agentName;
        private String status;
        private Integer issuesFound;

        public AgentExecutionDTO() {}

        public AgentExecutionDTO(String agentType, String agentName, String status, Integer issuesFound) {
            this.agentType = agentType;
            this.agentName = agentName;
            this.status = status;
            this.issuesFound = issuesFound;
        }

        public String getAgentType() { return agentType; }
        public void setAgentType(String agentType) { this.agentType = agentType; }
        public String getAgentName() { return agentName; }
        public void setAgentName(String agentName) { this.agentName = agentName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getIssuesFound() { return issuesFound; }
        public void setIssuesFound(Integer issuesFound) { this.issuesFound = issuesFound; }
    }

    /**
     * Review request DTO
     */
    class ReviewRequestDTO {
        private Long userId;
        private String code;
        private String language;
        private String fileName;
        private String visibility;

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getVisibility() { return visibility; }
        public void setVisibility(String visibility) { this.visibility = visibility; }
    }

    /**
     * Review result DTO
     */
    class ReviewResultDTO {
        private Long reviewId;
        private String status;
        private Integer totalIssues;
        private List<CodeIssue> issues;

        public static ReviewResultDTO builder() {
            return new ReviewResultDTO();
        }

        public ReviewResultDTO reviewId(Long reviewId) {
            this.reviewId = reviewId;
            return this;
        }

        public ReviewResultDTO status(String status) {
            this.status = status;
            return this;
        }

        public ReviewResultDTO totalIssues(Integer totalIssues) {
            this.totalIssues = totalIssues;
            return this;
        }

        public ReviewResultDTO issues(List<CodeIssue> issues) {
            this.issues = issues;
            return this;
        }

        public ReviewResultDTO build() {
            return this;
        }

        public Long getReviewId() { return reviewId; }
        public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getTotalIssues() { return totalIssues; }
        public void setTotalIssues(Integer totalIssues) { this.totalIssues = totalIssues; }
        public List<CodeIssue> getIssues() { return issues; }
        public void setIssues(List<CodeIssue> issues) { this.issues = issues; }
    }

    /**
     * Review detail DTO
     */
    class ReviewDetailDTO {
        private Long reviewId;
        private String codeContent;
        private String language;
        private String fileName;
        private String status;
        private Integer totalIssues;
        private List<CodeIssue> issues;
        private TeachingReport teachingReport;

        public Long getReviewId() { return reviewId; }
        public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
        public String getCodeContent() { return codeContent; }
        public void setCodeContent(String codeContent) { this.codeContent = codeContent; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getTotalIssues() { return totalIssues; }
        public void setTotalIssues(Integer totalIssues) { this.totalIssues = totalIssues; }
        public List<CodeIssue> getIssues() { return issues; }
        public void setIssues(List<CodeIssue> issues) { this.issues = issues; }
        public TeachingReport getTeachingReport() { return teachingReport; }
        public void setTeachingReport(TeachingReport teachingReport) {
            this.teachingReport = teachingReport;
        }
    }
}
