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
import com.codereview.ai.domain.service.ProjectReportGenerator;
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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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

    private static final long MAX_ZIP_SIZE_BYTES = 100L * 1024 * 1024;
    private static final long DEFAULT_CHUNK_SIZE_BYTES = 5L * 1024 * 1024;

    private final ProjectRepository projectRepository;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectReportRepository projectReportRepository;
    private final ObjectMapper objectMapper;
    private final MinioService minioService;
    private final KafkaProducerService kafkaProducerService;
    private final ProjectReportGenerator projectReportGenerator;

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
    @Transactional
    public ChunkedUploadSession initChunkedZipUpload(ChunkedUploadInitRequest request, Long userId) {
        validateChunkedInitRequest(request);

        Project project = Project.builder()
            .userId(userId)
            .projectName(request.getProjectName().trim())
            .description(request.getDescription())
            .uploadType(Project.UploadType.ZIP)
            .status(Project.ProjectStatus.PENDING)
            .visibility(request.getVisibility() != null ? request.getVisibility() : Project.ProjectVisibility.PRIVATE)
            .totalSize(request.getFileSize())
            .totalFiles(0)
            .analyzedFiles(0)
            .totalIssues(0)
            .build();

        project = projectRepository.save(project);

        String uploadId = UUID.randomUUID().toString();
        try {
            Path uploadDir = getUploadDir(uploadId);
            Files.createDirectories(uploadDir);
            saveUploadMetadata(uploadDir, project.getId(), userId, request);
        } catch (Exception e) {
            log.error("Failed to initialize chunked upload: projectId={}", project.getId(), e);
            throw new RuntimeException("Failed to initialize upload session", e);
        }

        ChunkedUploadSession session = new ChunkedUploadSession();
        session.setProjectId(project.getId());
        session.setUploadId(uploadId);
        session.setProjectName(project.getProjectName());
        session.setStatus(project.getStatus());
        session.setTotalChunks(request.getTotalChunks());
        session.setChunkSize(request.getChunkSize() != null ? request.getChunkSize() : DEFAULT_CHUNK_SIZE_BYTES);
        session.setMessage("Upload session initialized");

        log.info("Chunked ZIP upload initialized: userId={}, projectId={}, uploadId={}, chunks={}",
            userId, project.getId(), uploadId, request.getTotalChunks());

        return session;
    }

    @Override
    public void uploadZipProjectChunk(Long projectId, String uploadId, int chunkIndex, int totalChunks,
                                      InputStream inputStream, long chunkSize, Long userId) {
        validateChunkIndex(chunkIndex, totalChunks);
        Project project = getProjectForUser(projectId, userId);

        try {
            Path uploadDir = getUploadDir(uploadId);
            Properties metadata = loadUploadMetadata(uploadDir);
            validateUploadSession(metadata, project.getId(), userId, totalChunks);

            Path chunkPath = uploadDir.resolve(chunkIndex + ".part").normalize();
            if (!chunkPath.startsWith(uploadDir)) {
                throw new SecurityException("Invalid chunk path");
            }

            Files.copy(inputStream, chunkPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Project ZIP chunk uploaded: projectId={}, uploadId={}, chunk={}/{}, size={}",
                projectId, uploadId, chunkIndex + 1, totalChunks, chunkSize);

        } catch (Exception e) {
            log.error("Failed to upload project ZIP chunk: projectId={}, uploadId={}, chunk={}",
                projectId, uploadId, chunkIndex, e);
            throw new RuntimeException("Failed to upload chunk: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public Long completeChunkedZipUpload(Long projectId, String uploadId, String fileName, int totalChunks, Long userId) {
        Project project = getProjectForUser(projectId, userId);
        File mergedFile = null;

        try {
            Path uploadDir = getUploadDir(uploadId);
            Properties metadata = loadUploadMetadata(uploadDir);
            validateUploadSession(metadata, project.getId(), userId, totalChunks);

            String metadataFileName = metadata.getProperty("fileName");
            if (fileName == null || fileName.isBlank()) {
                fileName = metadataFileName;
            }
            validateZipFileName(fileName);

            mergedFile = File.createTempFile("project-merged-" + projectId + "-", ".zip");
            try (OutputStream outputStream = Files.newOutputStream(
                mergedFile.toPath(),
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            )) {
                for (int i = 0; i < totalChunks; i++) {
                    Path chunkPath = uploadDir.resolve(i + ".part").normalize();
                    if (!chunkPath.startsWith(uploadDir) || !Files.exists(chunkPath)) {
                        throw new IllegalArgumentException("Missing upload chunk: " + (i + 1));
                    }
                    Files.copy(chunkPath, outputStream);
                }
            }

            long mergedSize = mergedFile.length();
            if (mergedSize <= 0 || mergedSize > MAX_ZIP_SIZE_BYTES) {
                throw new IllegalArgumentException("Invalid merged ZIP size");
            }

            String safeFileName = sanitizeFileName(fileName);
            String objectName = "chunked/" + projectId + "/" + System.currentTimeMillis() + "-" + safeFileName;
            String storagePath = minioService.uploadProjectFile(mergedFile, objectName);

            project.setStoragePath(storagePath);
            project.setTotalSize(mergedSize);
            project.setStatus(Project.ProjectStatus.PENDING);
            projectRepository.save(project);

            kafkaProducerService.sendProjectAnalysisEvent(projectId);
            deleteDirectoryQuietly(uploadDir);

            log.info("Chunked ZIP upload completed: projectId={}, uploadId={}, storagePath={}, size={}",
                projectId, uploadId, storagePath, mergedSize);

            return projectId;

        } catch (Exception e) {
            log.error("Failed to complete chunked ZIP upload: projectId={}, uploadId={}", projectId, uploadId, e);
            throw new RuntimeException("Failed to complete upload: " + e.getMessage(), e);
        } finally {
            if (mergedFile != null && mergedFile.exists()) {
                try {
                    Files.deleteIfExists(mergedFile.toPath());
                } catch (Exception e) {
                    log.warn("Failed to delete merged upload file: {}", mergedFile.getAbsolutePath(), e);
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
        if (start >= allFiles.size()) {
            return new PageImpl<>(List.of(), pageable, allFiles.size());
        }
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
            dto.setFullMarkdownReport((String) metrics.get("fullMarkdownReport"));
            dto.setFileIssueDetails(metrics.get("fileIssueDetails"));
            Object severityDistribution = metrics.get("severityDistribution");
            if (severityDistribution instanceof Map<?, ?> severityMap) {
                dto.setSeverityDistribution(severityMap.entrySet().stream()
                    .collect(Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        Map.Entry::getValue
                    )));
            }
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
        log.info("Generating project report: projectId={}", projectId);

        Project project = getProjectById(projectId);
        projectReportGenerator.generateReport(projectId);

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

    private void validateChunkedInitRequest(ChunkedUploadInitRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Upload request is required");
        }
        if (request.getProjectName() == null || request.getProjectName().trim().isEmpty()) {
            throw new IllegalArgumentException("Project name is required");
        }
        validateZipFileName(request.getFileName());
        if (request.getFileSize() == null || request.getFileSize() <= 0) {
            throw new IllegalArgumentException("File size is required");
        }
        if (request.getFileSize() > MAX_ZIP_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 100MB limit");
        }
        if (request.getTotalChunks() == null || request.getTotalChunks() <= 0) {
            throw new IllegalArgumentException("Total chunks is required");
        }
        if (request.getTotalChunks() > 1000) {
            throw new IllegalArgumentException("Too many upload chunks");
        }
    }

    private void validateZipFileName(String fileName) {
        if (fileName == null || fileName.isBlank() || !fileName.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("Only ZIP files are supported");
        }
    }

    private void validateChunkIndex(int chunkIndex, int totalChunks) {
        if (totalChunks <= 0) {
            throw new IllegalArgumentException("Total chunks must be positive");
        }
        if (chunkIndex < 0 || chunkIndex >= totalChunks) {
            throw new IllegalArgumentException("Invalid chunk index");
        }
    }

    private Project getProjectForUser(Long projectId, Long userId) {
        return projectRepository.findByIdAndUserId(projectId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found or access denied: " + projectId));
    }

    private Path getUploadDir(String uploadId) {
        if (uploadId == null || !uploadId.matches("[a-fA-F0-9\\-]{36}")) {
            throw new IllegalArgumentException("Invalid upload session");
        }
        Path root = Path.of(System.getProperty("java.io.tmpdir"), "project-upload-chunks").normalize();
        return root.resolve(uploadId).normalize();
    }

    private void saveUploadMetadata(Path uploadDir, Long projectId, Long userId, ChunkedUploadInitRequest request) throws Exception {
        Properties metadata = new Properties();
        metadata.setProperty("projectId", String.valueOf(projectId));
        metadata.setProperty("userId", String.valueOf(userId));
        metadata.setProperty("fileName", request.getFileName());
        metadata.setProperty("fileSize", String.valueOf(request.getFileSize()));
        metadata.setProperty("totalChunks", String.valueOf(request.getTotalChunks()));
        metadata.setProperty("chunkSize", String.valueOf(request.getChunkSize() != null ? request.getChunkSize() : DEFAULT_CHUNK_SIZE_BYTES));

        try (OutputStream outputStream = Files.newOutputStream(
            uploadDir.resolve("upload.properties"),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        )) {
            metadata.store(outputStream, "Project chunked upload metadata");
        }
    }

    private Properties loadUploadMetadata(Path uploadDir) throws Exception {
        Path metadataPath = uploadDir.resolve("upload.properties").normalize();
        if (!metadataPath.startsWith(uploadDir) || !Files.exists(metadataPath)) {
            throw new IllegalArgumentException("Upload session not found");
        }

        Properties metadata = new Properties();
        try (InputStream inputStream = Files.newInputStream(metadataPath)) {
            metadata.load(inputStream);
        }
        return metadata;
    }

    private void validateUploadSession(Properties metadata, Long projectId, Long userId, int totalChunks) {
        Long metadataProjectId = Long.valueOf(metadata.getProperty("projectId", "0"));
        Long metadataUserId = Long.valueOf(metadata.getProperty("userId", "0"));
        int metadataTotalChunks = Integer.parseInt(metadata.getProperty("totalChunks", "0"));

        if (!metadataProjectId.equals(projectId) || !metadataUserId.equals(userId)) {
            throw new IllegalArgumentException("Upload session does not match current project");
        }
        if (metadataTotalChunks != totalChunks) {
            throw new IllegalArgumentException("Upload chunk count changed");
        }
    }

    private String sanitizeFileName(String fileName) {
        String safeName = Path.of(fileName).getFileName().toString();
        return safeName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void deleteDirectoryQuietly(Path directory) {
        try {
            if (!Files.exists(directory)) {
                return;
            }
            Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception e) {
                        log.warn("Failed to delete upload temp path: {}", path, e);
                    }
                });
        } catch (Exception e) {
            log.warn("Failed to clean upload temp directory: {}", directory, e);
        }
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
