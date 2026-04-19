package com.codereview.ai.api.controller;

import com.codereview.ai.api.dto.ProjectUploadResponse;
import com.think.platform.shared.common.result.Result;
import com.codereview.ai.domain.model.Project;
import com.codereview.ai.domain.service.ProjectService;
import com.codereview.ai.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Project Controller
 * Provides project-level code review API endpoints
 *
 * @author Code Review AI Team
 */
@Slf4j
@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final SecurityUtils securityUtils;

    /**
     * Upload a ZIP project file
     *
     * @param file ZIP file containing the project
     * @param projectName Project name
     * @param description Project description (optional)
     * @param visibility Project visibility (PRIVATE, PUBLIC, TEAM)
     * @param request HTTP request
     * @return Project ID
     */
    @PostMapping("/upload/zip")
    public Result<ProjectUploadResponse> uploadZipProject(
            @RequestParam("file") MultipartFile file,
            @RequestParam("projectName") String projectName,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "visibility", defaultValue = "PRIVATE") String visibility,
            HttpServletRequest httpRequest) {

        try {
            // Validate ZIP file
            if (file.isEmpty()) {
                return Result.error("File is empty");
            }

            String fileName = file.getOriginalFilename();
            if (fileName == null || !fileName.toLowerCase().endsWith(".zip")) {
                return Result.error("Only ZIP files are supported");
            }

            // Size limit: 100MB
            long maxSize = 100 * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return Result.error("File size exceeds 100MB limit");
            }

            Long userId = securityUtils.getCurrentUserId(httpRequest);

            log.info("ZIP project upload request: userId={}, projectName={}, fileSize={}",
                userId, projectName, file.getSize());

            ProjectService.ProjectUploadRequest uploadRequest = new ProjectService.ProjectUploadRequest();
            uploadRequest.setProjectName(projectName);
            uploadRequest.setDescription(description);
            uploadRequest.setVisibility(Project.ProjectVisibility.valueOf(visibility.toUpperCase()));
            uploadRequest.setUploadType(Project.UploadType.ZIP);

            Long projectId = projectService.uploadZipProject(
                file.getInputStream(),
                fileName,
                file.getSize(),
                uploadRequest,
                userId
            );

            ProjectUploadResponse response = new ProjectUploadResponse();
            response.setProjectId(projectId);
            response.setProjectName(projectName);
            response.setStatus(Project.ProjectStatus.PENDING);
            response.setMessage("Project uploaded successfully. Analysis will start shortly.");
            response.setUploadType(Project.UploadType.ZIP);
            response.setTotalFiles(0);

            return Result.success(response);

        } catch (Exception e) {
            log.error("ZIP project upload failed", e);
            return Result.error("Upload failed: " + e.getMessage());
        }
    }

    /**
     * Get project status
     *
     * @param projectId Project ID
     * @param request HTTP request
     * @return Project status
     */
    @GetMapping("/{projectId}/status")
    public Result<ProjectService.ProjectStatusDTO> getProjectStatus(
            @PathVariable Long projectId,
            HttpServletRequest request) {

        try {
            Long userId = securityUtils.getCurrentUserId(request);

            log.info("Get project status: userId={}, projectId={}", userId, projectId);

            ProjectService.ProjectStatusDTO status = projectService.getProjectStatus(projectId);

            return Result.success(status);

        } catch (IllegalArgumentException e) {
            log.error("Project not found: projectId={}", projectId);
            return Result.error(404, "Project not found: " + projectId);
        } catch (Exception e) {
            log.error("Failed to get project status", e);
            return Result.error("Failed to get status: " + e.getMessage());
        }
    }

    /**
     * Get project files
     *
     * @param projectId Project ID
     * @param page Page number (default 0)
     * @param size Page size (default 20)
     * @param request HTTP request
     * @return Project files
     */
    @GetMapping("/{projectId}/files")
    public Result<List<ProjectService.ProjectFileDTO>> getProjectFiles(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        try {
            Long userId = securityUtils.getCurrentUserId(request);

            log.info("Get project files: userId={}, projectId={}, page={}, size={}",
                userId, projectId, page, size);

            Pageable pageable = PageRequest.of(page, size, Sort.by("filePath"));
            Page<ProjectService.ProjectFileDTO> filesPage = projectService.getProjectFiles(projectId, pageable);

            return Result.success(filesPage.getContent());

        } catch (IllegalArgumentException e) {
            log.error("Project not found: projectId={}", projectId);
            return Result.error(404, "Project not found: " + projectId);
        } catch (Exception e) {
            log.error("Failed to get project files", e);
            return Result.error("Failed to get files: " + e.getMessage());
        }
    }

    /**
     * Get project report
     *
     * @param projectId Project ID
     * @param request HTTP request
     * @return Project report
     */
    @GetMapping("/{projectId}/report")
    public Result<ProjectService.ProjectReportDTO> getProjectReport(
            @PathVariable Long projectId,
            HttpServletRequest request) {

        try {
            Long userId = securityUtils.getCurrentUserId(request);

            log.info("Get project report: userId={}, projectId={}", userId, projectId);

            ProjectService.ProjectReportDTO report = projectService.getProjectReport(projectId);

            return Result.success(report);

        } catch (IllegalArgumentException e) {
            log.error("Project report not found: projectId={}", projectId);
            return Result.error(404, "Report not found for project: " + projectId);
        } catch (Exception e) {
            log.error("Failed to get project report", e);
            return Result.error("Failed to get report: " + e.getMessage());
        }
    }

    /**
     * Delete a project
     *
     * @param projectId Project ID
     * @param request HTTP request
     * @return Success message
     */
    @DeleteMapping("/{projectId}")
    public Result<String> deleteProject(
            @PathVariable Long projectId,
            HttpServletRequest request) {

        try {
            Long userId = securityUtils.getCurrentUserId(request);

            log.info("Delete project: userId={}, projectId={}", userId, projectId);

            projectService.deleteProject(projectId, userId);

            return Result.success("Project deleted successfully");

        } catch (IllegalArgumentException e) {
            log.error("Delete failed: {}", e.getMessage());
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to delete project", e);
            return Result.error("Delete failed: " + e.getMessage());
        }
    }

    /**
     * Get user's projects
     *
     * @param page Page number (default 0)
     * @param size Page size (default 10)
     * @param request HTTP request
     * @return User's projects
     */
    @GetMapping("/list")
    public Result<List<Project>> getUserProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        try {
            Long userId = securityUtils.getCurrentUserId(request);

            log.info("Get user projects: userId={}, page={}, size={}", userId, page, size);

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Project> projectsPage = projectService.getUserProjects(userId, pageable);

            return Result.success(projectsPage.getContent());

        } catch (Exception e) {
            log.error("Failed to get user projects", e);
            return Result.error("Failed to get projects: " + e.getMessage());
        }
    }

    /**
     * Request project report generation
     *
     * @param projectId Project ID
     * @param request HTTP request
     * @return Success message
     */
    @PostMapping("/{projectId}/generate-report")
    public Result<String> generateReport(
            @PathVariable Long projectId,
            HttpServletRequest request) {

        try {
            Long userId = securityUtils.getCurrentUserId(request);

            log.info("Generate project report: userId={}, projectId={}", userId, projectId);

            projectService.generateProjectReport(projectId);

            return Result.success("Report generation started");

        } catch (IllegalArgumentException e) {
            log.error("Project not found: projectId={}", projectId);
            return Result.error(404, "Project not found: " + projectId);
        } catch (Exception e) {
            log.error("Failed to generate report", e);
            return Result.error("Failed to generate report: " + e.getMessage());
        }
    }
}
