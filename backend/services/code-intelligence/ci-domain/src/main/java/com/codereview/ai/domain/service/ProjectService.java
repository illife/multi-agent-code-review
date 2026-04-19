package com.codereview.ai.domain.service;

import com.codereview.ai.domain.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Project Service Interface
 * Provides project-level code review functionality
 *
 * @author Code Review AI Team
 */
public interface ProjectService {

    /**
     * Upload a ZIP project file
     *
     * @param inputStream Input stream of the ZIP file
     * @param fileName Original file name
     * @param fileSize File size in bytes
     * @param request Project upload request
     * @param userId User ID
     * @return Project ID
     */
    Long uploadZipProject(InputStream inputStream, String fileName, long fileSize,
                         ProjectUploadRequest request, Long userId);

    /**
     * Get project by ID
     *
     * @param projectId Project ID
     * @return Project entity
     */
    Project getProjectById(Long projectId);

    /**
     * Get project status
     *
     * @param projectId Project ID
     * @return Project status DTO
     */
    ProjectStatusDTO getProjectStatus(Long projectId);

    /**
     * Get project files with pagination
     *
     * @param projectId Project ID
     * @param pageable Pagination parameters
     * @return Page of project files
     */
    Page<ProjectFileDTO> getProjectFiles(Long projectId, Pageable pageable);

    /**
     * Get all files for a project (non-paginated)
     *
     * @param projectId Project ID
     * @return List of project files
     */
    List<ProjectFileDTO> getAllProjectFiles(Long projectId);

    /**
     * Get project report
     *
     * @param projectId Project ID
     * @return Project report DTO
     */
    ProjectReportDTO getProjectReport(Long projectId);

    /**
     * Delete a project
     *
     * @param projectId Project ID
     * @param userId User ID (for permission check)
     */
    void deleteProject(Long projectId, Long userId);

    /**
     * Generate project report
     *
     * @param projectId Project ID
     */
    void generateProjectReport(Long projectId);

    /**
     * Get user's projects
     *
     * @param userId User ID
     * @param pageable Pagination parameters
     * @return Page of projects
     */
    Page<Project> getUserProjects(Long userId, Pageable pageable);

    /**
     * Update project status
     *
     * @param projectId Project ID
     * @param status New status
     */
    void updateProjectStatus(Long projectId, Project.ProjectStatus status);

    /**
     * Increment analyzed files count
     *
     * @param projectId Project ID
     */
    void incrementAnalyzedFiles(Long projectId);

    /**
     * Update total issues count
     *
     * @param projectId Project ID
     * @param issuesCount Number of issues to add
     */
    void updateTotalIssues(Long projectId, int issuesCount);

    // ============================================================
    // DTO Classes
    // ============================================================

    /**
     * Project Upload Request DTO
     */
    class ProjectUploadRequest {
        private String projectName;
        private String description;
        private Project.ProjectVisibility visibility;
        private Project.UploadType uploadType;
        private String sourceUrl;
        private String fileFilterConfig;

        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Project.ProjectVisibility getVisibility() { return visibility; }
        public void setVisibility(Project.ProjectVisibility visibility) { this.visibility = visibility; }
        public Project.UploadType getUploadType() { return uploadType; }
        public void setUploadType(Project.UploadType uploadType) { this.uploadType = uploadType; }
        public String getSourceUrl() { return sourceUrl; }
        public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
        public String getFileFilterConfig() { return fileFilterConfig; }
        public void setFileFilterConfig(String fileFilterConfig) { this.fileFilterConfig = fileFilterConfig; }
    }

    /**
     * Project Status DTO
     */
    class ProjectStatusDTO {
        private Long projectId;
        private String projectName;
        private Project.ProjectStatus status;
        private Integer totalFiles;
        private Integer analyzedFiles;
        private Integer totalIssues;
        private Double progress;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Project.UploadType uploadType;

        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }
        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }
        public Project.ProjectStatus getStatus() { return status; }
        public void setStatus(Project.ProjectStatus status) { this.status = status; }
        public Integer getTotalFiles() { return totalFiles; }
        public void setTotalFiles(Integer totalFiles) { this.totalFiles = totalFiles; }
        public Integer getAnalyzedFiles() { return analyzedFiles; }
        public void setAnalyzedFiles(Integer analyzedFiles) { this.analyzedFiles = analyzedFiles; }
        public Integer getTotalIssues() { return totalIssues; }
        public void setTotalIssues(Integer totalIssues) { this.totalIssues = totalIssues; }
        public Double getProgress() { return progress; }
        public void setProgress(Double progress) { this.progress = progress; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
        public Project.UploadType getUploadType() { return uploadType; }
        public void setUploadType(Project.UploadType uploadType) { this.uploadType = uploadType; }

        public int getProgressPercent() {
            if (totalFiles == null || totalFiles == 0) return 0;
            if (analyzedFiles == null) return 0;
            return (int) ((analyzedFiles * 100.0) / totalFiles);
        }
    }

    /**
     * Project File DTO
     */
    class ProjectFileDTO {
        private Long fileId;
        private String filePath;
        private String fileName;
        private String language;
        private Long fileSize;
        private Integer lineCount;
        private Boolean isAnalyzed;
        private Integer analysisPriority;
        private Long reviewId;
        private LocalDateTime createdAt;

        public Long getFileId() { return fileId; }
        public void setFileId(Long fileId) { this.fileId = fileId; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        public Integer getLineCount() { return lineCount; }
        public void setLineCount(Integer lineCount) { this.lineCount = lineCount; }
        public Boolean getIsAnalyzed() { return isAnalyzed; }
        public void setIsAnalyzed(Boolean isAnalyzed) { this.isAnalyzed = isAnalyzed; }
        public Integer getAnalysisPriority() { return analysisPriority; }
        public void setAnalysisPriority(Integer analysisPriority) { this.analysisPriority = analysisPriority; }
        public Long getReviewId() { return reviewId; }
        public void setReviewId(Long reviewId) { this.reviewId = reviewId; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    /**
     * Project Report DTO
     */
    class ProjectReportDTO {
        private Long projectId;
        private String summary;
        private Integer overallScore;
        private String riskLevel;
        private Map<String, Object> metrics;
        private String recommendations;
        private Map<String, Object> fileStatistics;
        private LocalDateTime createdAt;

        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public Integer getOverallScore() { return overallScore; }
        public void setOverallScore(Integer overallScore) { this.overallScore = overallScore; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public Map<String, Object> getMetrics() { return metrics; }
        public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }
        public String getRecommendations() { return recommendations; }
        public void setRecommendations(String recommendations) { this.recommendations = recommendations; }
        public Map<String, Object> getFileStatistics() { return fileStatistics; }
        public void setFileStatistics(Map<String, Object> fileStatistics) { this.fileStatistics = fileStatistics; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }

    /**
     * Project Upload Response DTO
     */
    class ProjectUploadResponse {
        private Long projectId;
        private String projectName;
        private Project.ProjectStatus status;
        private String message;
        private Project.UploadType uploadType;
        private Integer totalFiles;

        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }
        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }
        public Project.ProjectStatus getStatus() { return status; }
        public void setStatus(Project.ProjectStatus status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Project.UploadType getUploadType() { return uploadType; }
        public void setUploadType(Project.UploadType uploadType) { this.uploadType = uploadType; }
        public Integer getTotalFiles() { return totalFiles; }
        public void setTotalFiles(Integer totalFiles) { this.totalFiles = totalFiles; }
    }
}
