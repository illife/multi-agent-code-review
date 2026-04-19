package com.codereview.ai.domain.service.impl;

import com.codereview.ai.domain.infrastructure.kafka.KafkaProducerService;
import com.codereview.ai.domain.infrastructure.minio.MinioService;
import com.codereview.ai.domain.model.Project;
import com.codereview.ai.domain.model.ProjectFile;
import com.codereview.ai.domain.model.ProjectReport;
import com.codereview.ai.domain.repository.ProjectFileRepository;
import com.codereview.ai.domain.repository.ProjectReportRepository;
import com.codereview.ai.domain.repository.ProjectRepository;
import com.codereview.ai.domain.service.ProjectService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Project Service Implementation
 *
 * @author Code Review AI Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectReportRepository projectReportRepository;
    private final ObjectMapper objectMapper;
    private final MinioService minioService;
    private final KafkaProducerService kafkaProducerService;

    @Override
    @Transactional
    public Long uploadZipProject(InputStream inputStream, String fileName, long fileSize,
                                 ProjectUploadRequest request, Long userId) {
        log.info("Uploading ZIP project: userId={}, projectName={}, fileSize={}",
            userId, request.getProjectName(), fileSize);

        File tempFile = null;
        try {
            // Save InputStream to temporary file with unique name
            String suffix = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.')) : ".zip";
            String uniquePrefix = "project-upload-" + UUID.randomUUID().toString() + "-";
            tempFile = File.createTempFile(uniquePrefix, suffix);
            Path tempPath = tempFile.toPath();
            Files.copy(inputStream, tempPath, StandardCopyOption.REPLACE_EXISTING);

            // Generate a unique temporary ID for MinIO path
            String tempProjectId = "temp-" + UUID.randomUUID().toString();
            String objectName = "projects/" + tempProjectId + "/" + System.currentTimeMillis() + "-" + fileName;

            // Upload to MinIO first with temp ID
            String storagePath = minioService.uploadProjectFile(tempFile, objectName);
            log.info("Project uploaded to MinIO: storagePath={}", storagePath);

            // Create Project record with storage path
            Project.ProjectBuilder builder = Project.builder()
                .userId(userId)
                .projectName(request.getProjectName())
                .description(request.getDescription())
                .uploadType(Project.UploadType.ZIP)
                .status(Project.ProjectStatus.PENDING)
                .visibility(request.getVisibility() != null ? request.getVisibility() : Project.ProjectVisibility.PRIVATE)
                .storagePath(storagePath)
                .totalSize(fileSize)
                .totalFiles(0)
                .analyzedFiles(0)
                .totalIssues(0);

            // Only set fileFilterConfig if provided (it's optional)
            if (request.getFileFilterConfig() != null && !request.getFileFilterConfig().trim().isEmpty()) {
                builder.fileFilterConfig(request.getFileFilterConfig());
            }

            Project project = builder.build();
            project = projectRepository.save(project);
            Long projectId = project.getId();
            log.info("Project created: projectId={}", projectId);

            // Optionally rename in MinIO with actual projectId (not required, but cleaner)
            // For now, the temp path is fine since it's unique

            // Publish Kafka event for async processing
            kafkaProducerService.sendProjectAnalysisEvent(projectId);
            log.info("Project analysis event published: projectId={}", projectId);

            return projectId;

        } catch (Exception e) {
            log.error("Failed to upload ZIP project", e);
            throw new RuntimeException("Failed to upload project: " + e.getMessage(), e);
        } finally {
            // Clean up temporary file
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.deleteIfExists(tempFile.toPath());
                } catch (Exception e) {
                    log.warn("Failed to delete temp file: {}", tempFile.getPath(), e);
                }
            }
        }
    }

    @Override
    public Project getProjectById(Long projectId) {
        return projectRepository.findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
    }

    @Override
    public ProjectStatusDTO getProjectStatus(Long projectId) {
        Project project = getProjectById(projectId);

        double progress = 0.0;
        if (project.getTotalFiles() != null && project.getTotalFiles() > 0) {
            progress = (double) project.getAnalyzedFiles() / project.getTotalFiles();
        }

        ProjectStatusDTO dto = new ProjectStatusDTO();
        dto.setProjectId(project.getId());
        dto.setProjectName(project.getProjectName());
        dto.setStatus(project.getStatus());
        dto.setTotalFiles(project.getTotalFiles());
        dto.setAnalyzedFiles(project.getAnalyzedFiles());
        dto.setTotalIssues(project.getTotalIssues());
        dto.setProgress(progress);
        dto.setCreatedAt(project.getCreatedAt());
        dto.setUpdatedAt(project.getUpdatedAt());
        dto.setUploadType(project.getUploadType());

        return dto;
    }

    @Override
    public Page<ProjectFileDTO> getProjectFiles(Long projectId, Pageable pageable) {
        // Note: This requires custom query implementation in repository
        // For now, return all files and let the controller handle pagination
        List<ProjectFileDTO> allFiles = getAllProjectFiles(projectId);

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allFiles.size());
        List<ProjectFileDTO> pagedFiles = allFiles.subList(start, end);

        return new PageImpl<>(pagedFiles, pageable, allFiles.size());
    }

    @Override
    public List<ProjectFileDTO> getAllProjectFiles(Long projectId) {
        List<ProjectFile> files = projectFileRepository.findByProjectId(projectId);

        return files.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public ProjectReportDTO getProjectReport(Long projectId) {
        ProjectReport report = projectReportRepository.findByProjectId(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Report not found for project: " + projectId));

        try {
            Map<String, Object> metrics = objectMapper.readValue(
                report.getMetrics(),
                new TypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> fileStats = objectMapper.readValue(
                report.getFileStatistics(),
                new TypeReference<Map<String, Object>>() {}
            );

            ProjectReportDTO dto = new ProjectReportDTO();
            dto.setProjectId(report.getProjectId());
            dto.setSummary(report.getSummary());
            dto.setOverallScore(report.getOverallScore());
            dto.setRiskLevel(report.getRiskLevel());
            dto.setMetrics(metrics);
            dto.setRecommendations(report.getRecommendations());
            dto.setFileStatistics(fileStats);
            dto.setCreatedAt(report.getCreatedAt());

            return dto;

        } catch (Exception e) {
            log.error("Failed to parse report JSON: projectId={}", projectId, e);
            throw new RuntimeException("Failed to parse report data", e);
        }
    }

    @Override
    @Transactional
    public void deleteProject(Long projectId, Long userId) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied: " + projectId));

        // Delete file from MinIO storage
        if (project.getStoragePath() != null) {
            try {
                minioService.deleteObject(project.getStoragePath());
                log.info("File deleted from MinIO: storagePath={}", project.getStoragePath());
            } catch (Exception e) {
                log.warn("Failed to delete file from MinIO: storagePath={}", project.getStoragePath(), e);
            }
        }

        // Delete associated records (cascade will handle most)
        projectFileRepository.deleteByProjectId(projectId);
        projectReportRepository.deleteByProjectId(projectId);
        projectRepository.delete(project);

        log.info("Project deleted: projectId={}", projectId);
    }

    @Override
    @Transactional
    public void generateProjectReport(Long projectId) {
        // TODO: Implement report generation in ProjectReportGenerator
        log.info("Generating project report: projectId={}", projectId);

        // Placeholder - will be implemented by ProjectReportGenerator
        Project project = getProjectById(projectId);

        ProjectReport report = ProjectReport.builder()
            .projectId(projectId)
            .summary("Project analysis completed. " + project.getAnalyzedFiles() + " files analyzed.")
            .overallScore(75) // Placeholder
            .riskLevel("MEDIUM") // Placeholder
            .metrics("{}")
            .recommendations("Review critical issues first.")
            .fileStatistics("{}")
            .build();

        projectReportRepository.save(report);

        // Update project status
        project.setStatus(Project.ProjectStatus.COMPLETED);
        projectRepository.save(project);
    }

    @Override
    public Page<Project> getUserProjects(Long userId, Pageable pageable) {
        return projectRepository.findByUserId(userId, pageable);
    }

    @Override
    @Transactional
    public void updateProjectStatus(Long projectId, Project.ProjectStatus status) {
        Project project = getProjectById(projectId);
        project.setStatus(status);
        projectRepository.save(project);
    }

    @Override
    @Transactional
    public void incrementAnalyzedFiles(Long projectId) {
        Project project = getProjectById(projectId);
        project.setAnalyzedFiles((project.getAnalyzedFiles() != null ? project.getAnalyzedFiles() : 0) + 1);
        projectRepository.save(project);
    }

    @Override
    @Transactional
    public void updateTotalIssues(Long projectId, int issuesCount) {
        Project project = getProjectById(projectId);
        project.setTotalIssues((project.getTotalIssues() != null ? project.getTotalIssues() : 0) + issuesCount);
        projectRepository.save(project);
    }

    private ProjectFileDTO convertToDTO(ProjectFile file) {
        ProjectFileDTO dto = new ProjectFileDTO();
        dto.setFileId(file.getId());
        dto.setFilePath(file.getFilePath());
        dto.setFileName(file.getFileName());
        dto.setLanguage(file.getLanguage());
        dto.setFileSize(file.getFileSize());
        dto.setLineCount(file.getLineCount());
        dto.setIsAnalyzed(file.getIsAnalyzed());
        dto.setAnalysisPriority(file.getAnalysisPriority());
        dto.setReviewId(file.getReviewId());
        dto.setCreatedAt(file.getCreatedAt());
        return dto;
    }
}
