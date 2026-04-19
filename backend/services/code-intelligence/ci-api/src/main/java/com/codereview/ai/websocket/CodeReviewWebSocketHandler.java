package com.codereview.ai.websocket;

import com.codereview.ai.domain.agent.shared.AgentExecutionContext;
import com.codereview.ai.domain.agent.shared.AgentExecutionResult;
import com.codereview.ai.domain.agent.shared.AgentOrchestrationService;
import com.codereview.ai.domain.model.CodeIssue;
import com.codereview.ai.domain.model.CodeReview;
import com.codereview.ai.domain.model.TeachingReport;
import com.codereview.ai.domain.repository.CodeIssueRepository;
import com.codereview.ai.domain.repository.CodeReviewRepository;
import com.codereview.ai.domain.repository.TeachingReportRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

/**
 * WebSocket Code Review Handler
 *
 * Handles WebSocket connections for streaming code review
 * Supports JWT token authentication
 *
 * @author Code Review AI Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeReviewWebSocketHandler extends TextWebSocketHandler {

    private final CodeReviewRepository reviewRepository;
    private final CodeIssueRepository issueRepository;
    private final TeachingReportRepository teachingReportRepository;
    private final AgentOrchestrationService agentOrchestrationService;
    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            sessionManager.addSession(userId, session);
            log.info("Code review WebSocket connection established: userId={}, sessionId={}", userId, session.getId());
            sendWelcomeMessage(session);
        } else {
            log.warn("WebSocket connection failed: unable to get user info");
            session.close();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            sessionManager.removeSession(userId);
            log.info("Code review WebSocket connection closed: userId={}, sessionId={}, status={}",
                    userId, session.getId(), status);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        try {
            CodeReviewRequest request = objectMapper.readValue(payload, CodeReviewRequest.class);

            if ("heartbeat".equals(request.getType())) {
                sendHeartbeatResponse(session);
                return;
            }

            if ("submit".equals(request.getType())) {
                CompletableFuture.runAsync(() -> processCodeReview(session, request));
            } else if ("query".equals(request.getType())) {
                CompletableFuture.runAsync(() -> queryReviewStatus(session, request));
            } else {
                sendError(session, "Unknown request type: " + request.getType());
            }

        } catch (Exception e) {
            log.error("Failed to process WebSocket message", e);
            sendError(session, "Message processing failed: " + e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error: sessionId={}", session.getId(), exception);
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            sessionManager.removeSession(userId);
        }
    }

    private void processCodeReview(WebSocketSession session, CodeReviewRequest request) {
        String userId = getUserIdFromSession(session);

        CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting code review processing: userId={}, language={}, codeLength={}",
                        userId, request.getLanguage(), request.getCode().length());

                CodeReview review = CodeReview.builder()
                        .userId(Long.parseLong(userId))
                        .codeContent(request.getCode())
                        .language(request.getLanguage())
                        .fileName(request.getFileName())
                        .visibility(request.getVisibility() != null ? request.getVisibility() : "PRIVATE")
                        .status(CodeReview.ReviewStatus.PROCESSING)
                        .build();

                review = reviewRepository.save(review);
                final Long reviewId = review.getId();

                sendStartMessage(session, reviewId);

                // Send agent progress events
                sendAgentProgress(session, reviewId, "CODE_STANDARDS", "规范检查员", 10, "开始检查代码规范...");
                sendAgentProgress(session, reviewId, "ARCHITECTURE", "架构守卫者", 10, "开始检查架构设计...");
                sendAgentProgress(session, reviewId, "SECURITY", "安全审计员", 10, "开始检查安全问题...");

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

                // Process results and save issues
                Set<String> sentIssueKeys = new HashSet<>();
                int totalIssues = 0;
                Map<String, Integer> agentIssueCounts = new HashMap<>();

                for (AgentExecutionResult result : results) {
                    if (result.isSuccess() && result.getIssues() != null) {
                        String agentType = result.getAgentType();
                        agentIssueCounts.put(agentType, result.getIssues().size());

                        for (AgentExecutionResult.AgentIssue agentIssue : result.getIssues()) {
                            CodeIssue issue = convertAgentIssueToCodeIssue(agentIssue, reviewId);
                            issueRepository.save(issue);

                            String issueKey = issue.getRuleId() + ":" + (issue.getLineNumber() != null ? issue.getLineNumber() : "no-line");
                            if (sentIssueKeys.add(issueKey)) {
                                sendIssueFound(session, reviewId, issue);
                                totalIssues++;
                            }
                        }

                        sendAgentComplete(session, reviewId, agentType, result.getAgentName(),
                                agentIssueCounts.get(agentType), true);
                    } else {
                        sendAgentError(session, reviewId, result.getAgentType(), result.getAgentName(),
                                result.getError() != null ? result.getError() : "Execution failed");
                    }
                }

                // Finalize review
                finalizeReview(session, reviewId, totalIssues);

                log.info("Code review completed: userId={}, reviewId={}, totalIssues={}",
                        userId, reviewId, totalIssues);

            } catch (Exception e) {
                log.error("Code review processing failed: userId={}", userId, e);
                sendError(session, "Processing failed: " + e.getMessage());
            }
        });
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

    private void finalizeReview(WebSocketSession session, Long reviewId, int totalIssues) {
        try {
            CodeReview review = reviewRepository.findById(reviewId).orElse(null);
            if (review != null) {
                review.setStatus(CodeReview.ReviewStatus.COMPLETED);
                review.setTotalIssues(totalIssues);
                reviewRepository.save(review);

                sendComplete(session, reviewId, totalIssues);
            }
        } catch (Exception e) {
            log.error("Failed to finalize review: reviewId={}", reviewId, e);
        }
    }

    private void queryReviewStatus(WebSocketSession session, CodeReviewRequest request) {
        String userId = getUserIdFromSession(session);

        try {
            if (request.getReviewId() == null) {
                sendError(session, "Missing reviewId parameter");
                return;
            }

            CodeReview review = reviewRepository.findById(request.getReviewId())
                    .orElse(null);

            if (review == null) {
                sendError(session, "Review not found: " + request.getReviewId());
                return;
            }

            if (!review.getUserId().toString().equals(userId)) {
                sendError(session, "No permission to access this review");
                return;
            }

            sendStatusResponse(session, review);

        } catch (Exception e) {
            log.error("Failed to query review status: userId={}", userId, e);
            sendError(session, "Query failed: " + e.getMessage());
        }
    }

    private void sendWelcomeMessage(WebSocketSession session) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "welcome");
        message.put("message", "Code review WebSocket connection established");
        message.put("timestamp", System.currentTimeMillis());
        sendMessage(session, message);
    }

    private void sendHeartbeatResponse(WebSocketSession session) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "heartbeat");
        message.put("timestamp", System.currentTimeMillis());
        sendMessage(session, message);
    }

    private void sendStartMessage(WebSocketSession session, Long reviewId) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "start");
        message.put("reviewId", reviewId);
        message.put("timestamp", System.currentTimeMillis());
        sendMessage(session, message);
    }

    private void sendAgentProgress(WebSocketSession session, Long reviewId, String agentType,
                                    String agentName, int progress, String message) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "agent_progress");
        msg.put("reviewId", reviewId);
        msg.put("agentType", agentType);
        msg.put("agentName", agentName);
        msg.put("progress", progress);
        msg.put("message", message);
        msg.put("timestamp", System.currentTimeMillis());
        sendMessage(session, msg);
    }

    private void sendIssueFound(WebSocketSession session, Long reviewId, CodeIssue issue) {
        Map<String, Object> issueData = new HashMap<>();
        issueData.put("id", issue.getId());
        issueData.put("severity", issue.getSeverity());
        issueData.put("category", issue.getCategory());
        issueData.put("title", issue.getTitle());
        issueData.put("description", issue.getDescription());
        issueData.put("lineNumber", issue.getLineNumber());
        issueData.put("suggestion", issue.getSuggestion());
        issueData.put("agentType", issue.getAgentType());

        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "issue_found");
        msg.put("reviewId", reviewId);
        msg.put("issue", issueData);
        msg.put("timestamp", System.currentTimeMillis());
        sendMessage(session, msg);
    }

    private void sendAgentComplete(WebSocketSession session, Long reviewId, String agentType,
                                     String agentName, int issueCount, boolean success) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "agent_complete");
        msg.put("reviewId", reviewId);
        msg.put("agentType", agentType);
        msg.put("agentName", agentName);
        msg.put("issueCount", issueCount);
        msg.put("success", success);
        msg.put("timestamp", System.currentTimeMillis());
        sendMessage(session, msg);
    }

    private void sendAgentError(WebSocketSession session, Long reviewId, String agentType,
                                 String agentName, String errorMessage) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "agent_error");
        msg.put("reviewId", reviewId);
        msg.put("agentType", agentType);
        msg.put("agentName", agentName);
        msg.put("error", errorMessage);
        msg.put("timestamp", System.currentTimeMillis());
        sendMessage(session, msg);
    }

    private void sendComplete(WebSocketSession session, Long reviewId, int totalIssues) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "complete");
        message.put("reviewId", reviewId);
        message.put("totalIssues", totalIssues);
        message.put("timestamp", System.currentTimeMillis());
        sendMessage(session, message);
    }

    private void sendStatusResponse(WebSocketSession session, CodeReview review) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "status");
        message.put("reviewId", review.getId());
        message.put("status", review.getStatus().name());
        message.put("totalIssues", review.getTotalIssues());
        message.put("language", review.getLanguage());
        message.put("timestamp", System.currentTimeMillis());
        sendMessage(session, message);
    }

    private void sendError(WebSocketSession session, String errorMessage) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "error");
        message.put("error", errorMessage);
        message.put("timestamp", System.currentTimeMillis());
        sendMessage(session, message);
    }

    private void sendMessage(WebSocketSession session, Map<String, Object> message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));
        } catch (Exception e) {
            log.error("Failed to send WebSocket message", e);
        }
    }

    private String getUserIdFromSession(WebSocketSession session) {
        try {
            String uri = session.getUri().toString();
            if (uri == null || !uri.contains("?")) {
                return null;
            }

            String[] params = uri.split("\\?");
            if (params.length < 2) {
                return null;
            }

            String[] pairs = params[1].split("&");
            String token = null;

            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                    token = keyValue[1];
                    break;
                }
            }

            if (token == null || token.isEmpty()) {
                return null;
            }

            // Parse JWT token to extract userId (subject claim)
            try {
                String[] parts = token.split("\\.");
                if (parts.length < 2) {
                    log.warn("Invalid JWT token format");
                    return "1"; // Fallback to default user
                }

                // Decode payload (base64url)
                String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                // Extract "sub" field (subject = userId)
                String userId = extractJsonValue(payload, "sub");
                if (userId != null) {
                    return userId;
                }
            } catch (Exception e) {
                log.warn("Failed to parse JWT token, using default user", e);
            }

            // Fallback to default user
            return "1";

        } catch (Exception e) {
            log.error("Failed to extract user ID from WebSocket session", e);
            return null;
        }
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            return null;
        }

        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) {
            return null;
        }

        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        if (valueStart >= json.length()) {
            return null;
        }

        // Handle string values
        if (json.charAt(valueStart) == '"') {
            int valueEnd = json.indexOf('"', valueStart + 1);
            if (valueEnd == -1) {
                return null;
            }
            return json.substring(valueStart + 1, valueEnd);
        }

        // Handle number values
        int valueEnd = valueStart;
        while (valueEnd < json.length() &&
               (Character.isDigit(json.charAt(valueEnd)) || json.charAt(valueEnd) == '-' || json.charAt(valueEnd) == '+')) {
            valueEnd++;
        }

        return json.substring(valueStart, valueEnd);
    }

    public static class CodeReviewRequest {
        private String type;
        private Long reviewId;
        private String code;
        private String language;
        private String fileName;
        private String visibility;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public Long getReviewId() { return reviewId; }
        public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
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
