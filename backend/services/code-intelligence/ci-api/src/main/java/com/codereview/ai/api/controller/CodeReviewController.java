package com.codereview.ai.api.controller;

import com.think.platform.shared.common.result.Result;
import com.think.platform.shared.common.result.ResultCode;
import com.codereview.ai.domain.model.CodeIssue;
import com.codereview.ai.domain.model.CodeReview;
import com.codereview.ai.domain.service.CodeReviewService;
import com.codereview.ai.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Code Review Controller
 *
 * Provides code review API endpoints
 *
 * @author Code Review AI Team
 */
@Slf4j
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class CodeReviewController {

    private final CodeReviewService codeReviewService;
    private final SecurityUtils securityUtils;

    @PostMapping("/submit")
    public Result<CodeReviewService.ReviewResultDTO> submitReview(
            @RequestBody ReviewSubmitRequest request,
            HttpServletRequest httpRequest) {

        try {
            Long userId = securityUtils.getCurrentUserId(httpRequest);

            log.info("Received code review request: userId={}, language={}, codeLength={}",
                    userId, request.getLanguage(), request.getCode().length());

            CodeReviewService.ReviewRequestDTO requestDTO = new CodeReviewService.ReviewRequestDTO();
            requestDTO.setUserId(userId);
            requestDTO.setCode(request.getCode());
            requestDTO.setLanguage(request.getLanguage());
            requestDTO.setFileName(request.getFileName());
            requestDTO.setVisibility(request.getVisibility());

            CodeReviewService.ReviewResultDTO result = codeReviewService.submitReview(requestDTO);

            return Result.success(result);

        } catch (Exception e) {
            log.error("Code review processing failed", e);
            return Result.error("Code review failed: " + e.getMessage());
        }
    }

    @PostMapping("/submit-async")
    public Result<Long> submitReviewAsync(
            @RequestBody ReviewSubmitRequest request,
            HttpServletRequest httpRequest) {

        try {
            Long userId = securityUtils.getCurrentUserId(httpRequest);

            log.info("Received async code review request: userId={}, language={}", userId, request.getLanguage());

            CodeReviewService.ReviewRequestDTO requestDTO = new CodeReviewService.ReviewRequestDTO();
            requestDTO.setUserId(userId);
            requestDTO.setCode(request.getCode());
            requestDTO.setLanguage(request.getLanguage());
            requestDTO.setFileName(request.getFileName());
            requestDTO.setVisibility(request.getVisibility());

            Long reviewId = codeReviewService.submitReviewAsync(requestDTO);

            return Result.success(reviewId);

        } catch (Exception e) {
            log.error("Async code review submission failed", e);
            return Result.error("Submission failed: " + e.getMessage());
        }
    }

    @GetMapping("/{reviewId}")
    public Result<CodeReviewService.ReviewDetailDTO> getReviewDetail(
            @PathVariable Long reviewId,
            HttpServletRequest httpRequest) {

        try {
            Long userId = securityUtils.getCurrentUserId(httpRequest);

            log.info("Get review detail: userId={}, reviewId={}", userId, reviewId);

            CodeReviewService.ReviewDetailDTO detail = codeReviewService.getReviewDetail(reviewId);

            if (!detail.getStatus().equals("PENDING")) {
                CodeReview review = codeReviewService.getReview(reviewId);
                if (!review.getUserId().equals(userId) && !"PUBLIC".equals(review.getVisibility())) {
                    return Result.error(ResultCode.FORBIDDEN.getCode(), "No permission to access this review");
                }
            }

            return Result.success(detail);

        } catch (IllegalArgumentException e) {
            log.error("Review not found: reviewId={}", reviewId);
            return Result.error(ResultCode.NOT_FOUND.getCode(), "Review not found: " + reviewId);
        } catch (Exception e) {
            log.error("Failed to get review detail", e);
            return Result.error("Failed to get detail: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<Page<CodeReview>> getUserReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {

        try {
            Long userId = securityUtils.getCurrentUserId(httpRequest);

            log.info("Get user review list: userId={}, page={}, size={}", userId, page, size);

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<CodeReview> reviews = codeReviewService.getUserReviews(userId, pageable);

            return Result.success(reviews);

        } catch (Exception e) {
            log.error("Failed to get review list", e);
            return Result.error("Failed to get list: " + e.getMessage());
        }
    }

    @GetMapping("/{reviewId}/issues")
    public Result<List<CodeIssue>> getReviewIssues(
            @PathVariable Long reviewId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            HttpServletRequest httpRequest) {

        try {
            Long userId = securityUtils.getCurrentUserId(httpRequest);

            log.info("Get review issue list: userId={}, reviewId={}", userId, reviewId);

            CodeReview review = codeReviewService.getReview(reviewId);
            if (!review.getUserId().equals(userId) && !"PUBLIC".equals(review.getVisibility())) {
                return Result.error(ResultCode.FORBIDDEN.getCode(), "No permission to access this review");
            }

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "severity"));
            Page<CodeIssue> issuesPage = codeReviewService.getReviewIssues(reviewId, pageable);

            return Result.success(issuesPage.getContent());

        } catch (IllegalArgumentException e) {
            log.error("Review not found: reviewId={}", reviewId);
            return Result.error(ResultCode.NOT_FOUND.getCode(), "Review not found: " + reviewId);
        } catch (Exception e) {
            log.error("Failed to get issue list", e);
            return Result.error("Failed to get issues: " + e.getMessage());
        }
    }

    @GetMapping("/{reviewId}/agents")
    public Result<java.util.List<CodeReviewService.AgentExecutionDTO>> getAgentExecutions(
            @PathVariable Long reviewId,
            HttpServletRequest httpRequest) {

        try {
            Long userId = securityUtils.getCurrentUserId(httpRequest);

            log.info("Get agent executions: userId={}, reviewId={}", userId, reviewId);

            CodeReview review = codeReviewService.getReview(reviewId);
            if (!review.getUserId().equals(userId) && !"PUBLIC".equals(review.getVisibility())) {
                return Result.error(ResultCode.FORBIDDEN.getCode(), "No permission to access this review");
            }

            java.util.List<CodeReviewService.AgentExecutionDTO> agents = codeReviewService.getAgentExecutions(reviewId);

            return Result.success(agents);

        } catch (IllegalArgumentException e) {
            log.error("Review not found: reviewId={}", reviewId);
            return Result.error(ResultCode.NOT_FOUND.getCode(), "Review not found: " + reviewId);
        } catch (Exception e) {
            log.error("Failed to get agent executions", e);
            return Result.error("Failed to get agent executions: " + e.getMessage());
        }
    }

    @DeleteMapping("/{reviewId}")
    public Result<String> deleteReview(
            @PathVariable Long reviewId,
            HttpServletRequest httpRequest) {

        try {
            Long userId = securityUtils.getCurrentUserId(httpRequest);

            log.info("Delete review: userId={}, reviewId={}", userId, reviewId);

            codeReviewService.deleteReview(reviewId, userId);

            return Result.success("Deleted successfully");

        } catch (IllegalArgumentException e) {
            log.error("Delete failed: {}", e.getMessage());
            return Result.error(ResultCode.VALID_ERROR.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Failed to delete review", e);
            return Result.error("Delete failed: " + e.getMessage());
        }
    }

    /**
     * Get review detail endpoint (alias for /{reviewId})
     * Frontend calls /{reviewId}/detail but backend uses /{reviewId}
     */
    @GetMapping("/{reviewId}/detail")
    public Result<CodeReviewService.ReviewDetailDTO> getReviewDetailAlias(
            @PathVariable Long reviewId,
            HttpServletRequest httpRequest) {
        return getReviewDetail(reviewId, httpRequest);
    }

    /**
     * Download teaching report as Markdown
     */
    @GetMapping("/{reviewId}/teaching-report/download")
    public ResponseEntity<InputStreamResource> downloadTeachingReport(
            @PathVariable Long reviewId,
            HttpServletRequest httpRequest) {

        try {
            Long userId = securityUtils.getCurrentUserId(httpRequest);

            log.info("Download teaching report: userId={}, reviewId={}", userId, reviewId);

            // Check permission
            CodeReview review = codeReviewService.getReview(reviewId);
            if (!review.getUserId().equals(userId) && !"PUBLIC".equals(review.getVisibility())) {
                return ResponseEntity.status(403).build();
            }

            String markdown = codeReviewService.getTeachingReportMarkdown(reviewId);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(
                    markdown.getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment",
                    "teaching-report-" + reviewId + ".md");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(inputStream));

        } catch (IllegalArgumentException e) {
            log.error("Teaching report not found: reviewId={}", reviewId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to download teaching report", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Download full review report (currently as Markdown, can be extended to PDF)
     */
    @GetMapping("/{reviewId}/report/pdf")
    public ResponseEntity<InputStreamResource> downloadReviewReport(
            @PathVariable Long reviewId,
            HttpServletRequest httpRequest) {

        try {
            Long userId = securityUtils.getCurrentUserId(httpRequest);

            log.info("Download review report: userId={}, reviewId={}", userId, reviewId);

            // Check permission
            CodeReview review = codeReviewService.getReview(reviewId);
            if (!review.getUserId().equals(userId) && !"PUBLIC".equals(review.getVisibility())) {
                return ResponseEntity.status(403).build();
            }

            String markdown = codeReviewService.getFullReportMarkdown(reviewId);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(
                    markdown.getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment",
                    "review-report-" + reviewId + ".md");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(inputStream));

        } catch (IllegalArgumentException e) {
            log.error("Review not found: reviewId={}", reviewId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to download review report", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    public static class ReviewSubmitRequest {
        private String code;
        private String language;
        private String fileName;
        private String visibility;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getVisibility() { return visibility; }
        public void setVisibility(String visibility) { this.visibility = visibility; }
    }
}
