package com.codereview.ai.websocket;

import com.codereview.ai.domain.model.ProjectReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

/**
 * Project WebSocket Handler
 * Handles WebSocket notifications for project analysis progress
 *
 * @author Code Review AI Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectWebSocketHandler {

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Notify project upload started
     */
    public void notifyProjectUploadStart(Long projectId, String projectName, int totalFiles, String userId) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "project_upload_start");
        message.put("projectId", projectId);
        message.put("projectName", projectName);
        message.put("totalFiles", totalFiles);
        message.put("timestamp", System.currentTimeMillis());

        sendToUser(userId, message);
    }

    /**
     * Notify file analysis started
     */
    public void notifyFileAnalysisStart(Long projectId, String fileName, int currentFile, int totalFiles, String userId) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "file_analysis_start");
        message.put("projectId", projectId);
        message.put("fileName", fileName);
        message.put("currentFile", currentFile);
        message.put("totalFiles", totalFiles);
        message.put("progress", totalFiles > 0 ? (currentFile * 100.0 / totalFiles) : 0);
        message.put("timestamp", System.currentTimeMillis());

        sendToUser(userId, message);
    }

    /**
     * Notify file analysis completed
     */
    public void notifyFileAnalysisComplete(Long projectId, String fileName, int issuesFound, String userId) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "file_analysis_complete");
        message.put("projectId", projectId);
        message.put("fileName", fileName);
        message.put("issuesFound", issuesFound);
        message.put("timestamp", System.currentTimeMillis());

        sendToUser(userId, message);
    }

    /**
     * Notify project analysis completed
     */
    public void notifyProjectComplete(Long projectId, ProjectReport report, String userId) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "project_analysis_complete");
        message.put("projectId", projectId);
        message.put("report", convertReportToMap(report));
        message.put("timestamp", System.currentTimeMillis());

        sendToUser(userId, message);
    }

    /**
     * Notify project error
     */
    public void notifyProjectError(Long projectId, String error, String userId) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "project_error");
        message.put("projectId", projectId);
        message.put("error", error);
        message.put("timestamp", System.currentTimeMillis());

        sendToUser(userId, message);
    }

    /**
     * Notify project status update
     */
    public void notifyProjectStatusUpdate(Long projectId, String status, int analyzedFiles, int totalFiles, String userId) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "project_status_update");
        message.put("projectId", projectId);
        message.put("status", status);
        message.put("analyzedFiles", analyzedFiles);
        message.put("totalFiles", totalFiles);
        message.put("progress", totalFiles > 0 ? (analyzedFiles * 100.0 / totalFiles) : 0);
        message.put("timestamp", System.currentTimeMillis());

        sendToUser(userId, message);
    }

    /**
     * Send message to specific user
     */
    private void sendToUser(String userId, Map<String, Object> message) {
        WebSocketSession session = sessionManager.getSession(userId);
        if (session != null && session.isOpen()) {
            try {
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
                log.debug("WebSocket message sent: userId={}, type={}", userId, message.get("type"));
            } catch (Exception e) {
                log.error("Failed to send WebSocket message: userId={}", userId, e);
            }
        } else {
            log.debug("No active WebSocket session for user: {}", userId);
        }
    }

    /**
     * Broadcast message to all connected sessions
     */
    public void broadcast(Long projectId, Map<String, Object> message) {
        // For public projects, we might want to broadcast to multiple viewers
        // For now, this is a placeholder for future functionality
        log.debug("Broadcast not yet implemented: projectId={}", projectId);
    }

    /**
     * Convert ProjectReport to Map for JSON serialization
     */
    private Map<String, Object> convertReportToMap(ProjectReport report) {
        Map<String, Object> reportMap = new HashMap<>();
        reportMap.put("projectId", report.getProjectId());
        reportMap.put("summary", report.getSummary());
        reportMap.put("overallScore", report.getOverallScore());
        reportMap.put("riskLevel", report.getRiskLevel());
        reportMap.put("metrics", report.getMetrics());
        reportMap.put("recommendations", report.getRecommendations());
        reportMap.put("fileStatistics", report.getFileStatistics());
        reportMap.put("createdAt", report.getCreatedAt());
        return reportMap;
    }

    /**
     * Message type enum for project notifications
     */
    public enum ProjectMessageType {
        PROJECT_UPLOAD_START,
        FILE_ANALYSIS_START,
        FILE_ANALYSIS_COMPLETE,
        PROJECT_ANALYSIS_COMPLETE,
        PROJECT_ERROR,
        PROJECT_STATUS_UPDATE
    }
}
