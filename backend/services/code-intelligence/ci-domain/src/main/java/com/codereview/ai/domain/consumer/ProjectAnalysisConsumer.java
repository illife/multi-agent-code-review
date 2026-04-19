package com.codereview.ai.domain.consumer;


import com.codereview.ai.domain.agent.shared.AgentExecutionContext;
import com.codereview.ai.domain.agent.shared.AgentExecutionResult;
import com.codereview.ai.domain.agent.shared.AgentOrchestrationService;
import com.codereview.ai.domain.model.AgentTask;
import com.codereview.ai.domain.model.CodeIssue;
import com.codereview.ai.domain.model.CodeReview;
import com.codereview.ai.domain.model.Project;
import com.codereview.ai.domain.model.ProjectFile;
import com.codereview.ai.domain.repository.ProjectFileRepository;
import com.codereview.ai.domain.repository.ProjectRepository;
import com.codereview.ai.domain.repository.ProjectReportRepository;
import com.codereview.ai.domain.service.ProjectFileMetadata;
import com.codereview.ai.domain.service.ProjectFileScanner;
import com.codereview.ai.domain.service.ProjectReportGenerator;
import com.codereview.ai.domain.infrastructure.kafka.KafkaProducerService;
import com.codereview.ai.domain.infrastructure.minio.MinioService;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Project Analysis Consumer
 * Processes project analysis requests from Kafka
 *
 * @author Code Review AI Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectAnalysisConsumer {

    private final ProjectRepository projectRepository;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectFileScanner fileScanner;
    private final ProjectReportGenerator projectReportGenerator;
    private final ProjectReportRepository projectReportRepository;
    private final MinioService minioService;
    private final KafkaProducerService kafkaProducerService;
    private final AgentOrchestrationService agentOrchestrationService;

    // TODO: Add WebSocket notification support via event mechanism (domain -> api)
    // WebSocket notifications should be handled in the api module to avoid circular dependencies

    @PostConstruct
    public void init() {
        log.info("====================================");
        log.info("✅ ProjectAnalysisConsumer initialized");
        log.info("====================================");
        log.info("Kafka listener active on topic: {}", KafkaProducerService.PROJECT_ANALYSIS_TOPIC);
        log.info("====================================");
    }

    /**
     * Process project analysis from Kafka message
     */
    @KafkaListener(
            topics = "project-analysis",
            groupId = "project-analysis-group",
            concurrency = "1"
    )
    public void processProject(String message) {
        log.info("Received project analysis message: {}", message);

        Long projectId;
        try {
            projectId = Long.parseLong(message);
        } catch (NumberFormatException e) {
            log.error("Invalid project ID in message: {}", message);
            return;
        }

        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            log.warn("Project not found: {}", projectId);
            return;
        }

        // Check if already processed
        if (project.getStatus() == Project.ProjectStatus.COMPLETED) {
            log.info("Project already completed: {}", projectId);
            return;
        }

        processProjectAnalysis(project);
    }

    /**
     * Process project analysis logic
     */
    private void processProjectAnalysis(Project project) {
        Path tempDir = null;
        try {
            Long projectId = project.getId();

            // Update status to ANALYZING
            project.setStatus(Project.ProjectStatus.ANALYZING);
            projectRepository.save(project);

            log.info("Project analysis started: projectId={}, projectName={}", projectId, project.getProjectName());

            // Download ZIP from MinIO
            log.info("Downloading project ZIP from MinIO: projectId={}, storagePath={}", projectId, project.getStoragePath());
            File zipFile = minioService.downloadToTempFile(project.getStoragePath());

            // Extract ZIP to temp directory
            tempDir = extractZipToTempDirectory(zipFile);

            // Scan project files
            log.info("Scanning project files: projectId={}, tempDir={}", projectId, tempDir);
            List<ProjectFileMetadata> fileMetadataList = fileScanner.scanProject(tempDir);

            // Create ProjectFile records
            List<ProjectFile> projectFiles = createProjectFiles(projectId, fileMetadataList);
            project.setTotalFiles(projectFiles.size());
            projectRepository.save(project);

            log.info("Project files discovered: projectId={}, totalFiles={}", projectId, projectFiles.size());

            // Analyze each file with AI Agents
            int totalIssues = 0;
            int processedFiles = 0;

            for (ProjectFile file : projectFiles) {
                try {
                    log.info("AI analyzing file: projectId={}, fileId={}, fileName={}", projectId, file.getId(), file.getFileName());
                    int fileIssues = analyzeFileWithAI(project, file, tempDir);

                    if (fileIssues > 0) {
                        totalIssues += fileIssues;
                    }

                    file.setIsAnalyzed(true);
                    projectFileRepository.save(file);

                    processedFiles++;

                    log.info("File analysis progress: projectId={}, fileId={}, fileName={}, progress={}/{}, issues={}",
                            projectId, file.getId(), file.getFileName(), processedFiles, projectFiles.size(), fileIssues);

                } catch (Exception e) {
                    log.error("File analysis failed: projectId={}, fileId={}", projectId, file.getId(), e);
                    // Mark as analyzed even if failed, so we don't retry indefinitely
                    file.setIsAnalyzed(true);
                    projectFileRepository.save(file);
                }
            }

            project.setAnalyzedFiles(processedFiles);
            project.setTotalIssues(totalIssues);
            projectRepository.save(project);

            // Perform architecture analysis
            log.info("Starting architecture analysis: projectId={}", projectId);
            ProjectArchitectureInfo architectureInfo = analyzeProjectArchitecture(tempDir, projectFiles, projectId);
            log.info("Architecture analysis complete: projectId={}, layers={}, hasIssues={}",
                    projectId, architectureInfo.getLayers().size(), architectureInfo.isHasArchitecturalIssues());

            // Delete old report if exists (to avoid duplicate records)
            if (projectReportRepository.existsByProjectId(projectId)) {
                log.info("Deleting old report for project: {}", projectId);
                projectReportRepository.deleteByProjectId(projectId);
            }

            // Generate and save report with architecture info
            log.info("Generating project report: projectId={}", projectId);
            var report = projectReportGenerator.generateReport(projectId, architectureInfo);

            // Update status to COMPLETED
            project.setStatus(Project.ProjectStatus.COMPLETED);
            projectRepository.save(project);

            log.info("Project analysis completed: projectId={}, totalFiles={}, totalIssues={}",
                    projectId, projectFiles.size(), totalIssues);

        } catch (Exception e) {
            log.error("Project analysis failed: projectId={}", project.getId(), e);

            project.setStatus(Project.ProjectStatus.FAILED);
            projectRepository.save(project);

            log.error("Project analysis failed: projectId={}", project.getId(), e);

        } finally {
            // Clean up temp directory
            if (tempDir != null) {
                try {
                    deleteDirectory(tempDir);
                } catch (Exception e) {
                    log.warn("Failed to clean up temp directory: {}", tempDir, e);
                }
            }
        }
    }

    /**
     * Analyze a single file using AI Agent Orchestrator
     * Reads file content and performs real code analysis with ArchitectureGuardian
     */
    private int analyzeFileWithAI(Project project, ProjectFile file, Path tempDir) {
        try {
            // Read file content from extracted temp directory
            Path filePath = tempDir.resolve(file.getFilePath());
            if (!Files.exists(filePath)) {
                log.warn("File not found in temp directory: {}", filePath);
                return 0;
            }

            String codeContent = Files.readString(filePath);
            if (codeContent == null || codeContent.trim().isEmpty()) {
                log.debug("File is empty: {}", file.getFileName());
                return 0;
            }

            // Skip binary files and very large files
            if (codeContent.length() > 100000) { // > 100KB
                log.info("Skipping large file: fileName={}, size={}", file.getFileName(), codeContent.length());
                return 0;
            }

            log.info("Performing AI analysis: fileName={}, language={}, size={} bytes",
                    file.getFileName(), file.getLanguage(), codeContent.length());

            // Build execution context for AI analysis
            AgentExecutionContext context = AgentExecutionContext.builder()
                    .requestId("project-file-" + project.getId() + "-" + file.getId())
                    .userId(project.getUserId())
                    .code(codeContent)
                    .language(file.getLanguage() != null ? file.getLanguage() : "UNKNOWN")
                    .filePath(file.getFilePath())
                    .build();

            // Execute AI code analysis
            List<AgentExecutionResult> results = agentOrchestrationService.executeCodeReview(context);

            // Count issues found
            int issueCount = 0;
            for (AgentExecutionResult result : results) {
                if (result.isSuccess() && result.getIssues() != null) {
                    issueCount += result.getIssues().size();

                    // Log issues found (we could save these to database in future)
                    for (AgentExecutionResult.AgentIssue issue : result.getIssues()) {
                        log.info("Issue found: file={}, severity={}, line={}, title={}",
                                file.getFileName(), issue.getSeverity(), issue.getLineNumber(), issue.getTitle());
                    }
                }
            }

            log.info("AI analysis complete: fileName={}, issuesFound={}", file.getFileName(), issueCount);
            return issueCount;

        } catch (Exception e) {
            log.error("File analysis failed: fileId={}, fileName={}", file.getId(), file.getFileName(), e);
            return 0;
        }
    }

    /**
     * Extract ZIP file to temporary directory
     */
    private Path extractZipToTempDirectory(File zipFile) throws Exception {
        Path tempDir = Files.createTempDirectory("project-extract-");

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                Path outputPath = tempDir.resolve(entry.getName());
                // Ensure the output path is within tempDir (security check)
                if (!outputPath.startsWith(tempDir)) {
                    throw new SecurityException("ZIP entry outside target directory: " + entry.getName());
                }

                Files.createDirectories(outputPath.getParent());
                Files.copy(zis, outputPath);
                zis.closeEntry();
            }
        }

        log.info("ZIP extracted to: {}", tempDir);
        return tempDir;
    }

    /**
     * Create ProjectFile records from scanned metadata
     */
    private List<ProjectFile> createProjectFiles(Long projectId, List<ProjectFileMetadata> metadataList) {
        return metadataList.stream()
                .map(meta -> {
                    ProjectFile file = ProjectFile.builder()
                            .projectId(projectId)
                            .filePath(meta.getRelativePath())
                            .fileName(meta.getFileName())
                            .language(meta.getLanguage())
                            .fileSize(meta.getSize())
                            .lineCount(meta.getLineCount())
                            .isAnalyzed(false)
                            .analysisPriority(calculatePriority(meta))
                            .build();
                    return projectFileRepository.save(file);
                })
                .toList();
    }

    /**
     * Calculate analysis priority based on file metadata
     */
    private int calculatePriority(ProjectFileMetadata metadata) {
        int priority = 0;

        if (metadata.isEntryPoint()) {
            priority += 100;
        }
        if (metadata.isInSrcDir()) {
            priority += 50;
        }
        if (!metadata.isTestFile() && !metadata.isConfigFile()) {
            priority += 20;
        }

        // Smaller files get slightly higher priority (faster feedback)
        if (metadata.getSize() < 10000) { // < 10KB
            priority += 10;
        }

        return priority;
    }

    /**
     * Analyze project architecture using AI Agents
     * Uses ALL AI agents (CodeStandardsInspector, ArchitectureGuardian, SecurityAuditor, PerformanceOptimizer)
     * to provide comprehensive project analysis with detailed markdown report
     */
    private ProjectArchitectureInfo analyzeProjectArchitecture(Path projectRoot, List<ProjectFile> files, Long projectId) {
        log.info("🤖 AI-Powered comprehensive analysis: fileCount={}", files.size());

        ProjectArchitectureInfo info = new ProjectArchitectureInfo();

        // Enhanced data structures
        List<FileIssueDetail> fileIssueDetails = new ArrayList<>();
        Map<String, List<AgentIssueSummary>> issuesByAgentMap = new HashMap<>();
        Map<String, Integer> issuesByAgent = new HashMap<>();
        Map<String, Integer> issuesBySeverity = new HashMap<>();
        Map<String, Integer> issuesByCategory = new HashMap<>();
        List<String> allIssues = new ArrayList<>();

        // For markdown generation
        StringBuilder markdownReport = new StringBuilder();

        try {
            // Sample key files for comprehensive analysis
            List<ProjectFile> sampleFiles = files.stream()
                    .filter(f -> f.getLanguage() != null && !f.getLanguage().equals("UNKNOWN"))
                    .limit(15) // Increased to 15 for better coverage
                    .toList();

            int totalIssues = 0;
            int filesAnalyzed = 0;
            int filesWithIssues = 0;

            // Build markdown report header
            markdownReport.append("# 🤖 AI 多智能体代码审查报告\n\n");
            markdownReport.append("> **生成时间**: ").append(new java.util.Date()).append("\n\n");
            markdownReport.append("---\n\n");

            for (ProjectFile file : sampleFiles) {
                try {
                    Path filePath = projectRoot.resolve(file.getFilePath());
                    if (!Files.exists(filePath)) {
                        log.debug("File not found for analysis: {}", filePath);
                        continue;
                    }

                    String codeContent = Files.readString(filePath);

                    // Skip binary files and very large files
                    if (codeContent.length() > 100000) { // > 100KB
                        log.info("Skipping large file in arch analysis: fileName={}, size={}",
                                file.getFileName(), codeContent.length());
                        continue;
                    }

                    // Build context for AI analysis
                    AgentExecutionContext context = AgentExecutionContext.builder()
                            .requestId("project-analysis-" + projectId + "-" + file.getId())
                            .userId(1L)
                            .code(codeContent)
                            .language(file.getLanguage() != null ? file.getLanguage() : "UNKNOWN")
                            .filePath(file.getFilePath())
                            .build();

                    // Execute ALL AI agents for comprehensive analysis
                    List<AgentExecutionResult> results = agentOrchestrationService.executeCodeReview(context);

                    // Process file issues
                    FileIssueDetail fileDetail = new FileIssueDetail();
                    fileDetail.setFileName(file.getFileName());
                    fileDetail.setFilePath(file.getFilePath());
                    fileDetail.setLanguage(file.getLanguage());
                    fileDetail.setIssues(new ArrayList<>());

                    boolean fileHasIssues = false;

                    // Collect ALL issues from ALL agents
                    for (AgentExecutionResult result : results) {
                        if (result.isSuccess() && result.getIssues() != null) {
                            String agentType = result.getAgentType();
                            String agentName = result.getAgentName();

                            for (AgentExecutionResult.AgentIssue issue : result.getIssues()) {
                                totalIssues++;

                                // Track statistics
                                issuesByAgent.merge(agentName, 1, Integer::sum);
                                issuesBySeverity.merge(issue.getSeverity().name(), 1, Integer::sum);
                                issuesByCategory.merge(issue.getCategory(), 1, Integer::sum);

                                // Create detailed issue
                                IssueDetail issueDetail = new IssueDetail();
                                issueDetail.setAgentName(agentName);
                                issueDetail.setSeverity(issue.getSeverity().name());
                                issueDetail.setCategory(issue.getCategory());
                                issueDetail.setTitle(issue.getTitle());
                                issueDetail.setDescription(issue.getDescription());
                                issueDetail.setSuggestion(issue.getSuggestion());
                                issueDetail.setLineNumber(issue.getLineNumber());

                                // Extract code snippet if line number is available
                                if (issue.getLineNumber() != null && issue.getLineNumber() > 0) {
                                    issueDetail.setCodeSnippet(extractCodeSnippet(codeContent, issue.getLineNumber(), 3));
                                }

                                fileDetail.getIssues().add(issueDetail);
                                fileHasIssues = true;

                                // Format for markdown
                                String severityIcon = getSeverityIcon(issue.getSeverity().name());
                                String issueMarkdown = String.format(
                                    "#### %s %s\n\n**Agent**: %s | **Severity**: %s | **Category**: %s\n\n**Description**: %s\n\n",
                                    severityIcon, issue.getTitle(), agentName, issue.getSeverity().name(),
                                    issue.getCategory(), issue.getDescription()
                                );
                                if (issue.getSuggestion() != null && !issue.getSuggestion().isEmpty()) {
                                    issueMarkdown += String.format("**💡 建议**: %s\n\n", issue.getSuggestion());
                                }
                                if (issueDetail.getCodeSnippet() != null) {
                                    issueMarkdown += String.format("**📍 代码位置**:\n```%s\n%s\n```\n\n",
                                        getLanguageAlias(file.getLanguage()), issueDetail.getCodeSnippet());
                                }
                                allIssues.add(issueMarkdown);

                                log.info("🤖 AI Issue: agent={}, file={}, severity={}, category={}, title={}",
                                        agentName, file.getFileName(), issue.getSeverity(),
                                        issue.getCategory(), issue.getTitle());
                            }
                        }
                    }

                    if (fileHasIssues) {
                        fileDetail.setTotalIssues(fileDetail.getIssues().size());
                        fileIssueDetails.add(fileDetail);
                        filesWithIssues++;
                    }

                    filesAnalyzed++;

                } catch (Exception e) {
                    log.warn("Failed to analyze file: {}", file.getFileName(), e);
                }
            }

            // Generate comprehensive markdown report
            generateMarkdownReport(markdownReport, files, filesAnalyzed, filesWithIssues, totalIssues,
                issuesByAgent, issuesBySeverity, issuesByCategory, fileIssueDetails);

            // Set basic info for backward compatibility
            info.setProjectType("AI-COMPREHENSIVE-ANALYSIS");
            info.setFileIssueDetails(fileIssueDetails);
            info.setSeverityDistribution(issuesBySeverity);

            // Build agent summaries
            for (Map.Entry<String, Integer> entry : issuesByAgent.entrySet()) {
                AgentIssueSummary summary = new AgentIssueSummary();
                summary.setAgentName(entry.getKey());
                summary.setIssueCount(entry.getValue());
                summary.setSeverityBreakdown(new HashMap<>());
                summary.setCategoryBreakdown(new HashMap<>());
                summary.setTopIssues(new ArrayList<>());
                issuesByAgentMap.put(entry.getKey(), List.of(summary));
            }
            info.setIssuesByAgent(issuesByAgentMap);

            // Layer structure with agent stats
            Map<String, Integer> layers = new HashMap<>();
            layers.put("total_files", files.size());
            layers.put("analyzed_files", filesAnalyzed);
            layers.put("files_with_issues", filesWithIssues);
            layers.put("total_issues", totalIssues);

            // Add agent-specific stats
            for (Map.Entry<String, Integer> entry : issuesByAgent.entrySet()) {
                String key = entry.getKey().toLowerCase().replace(" ", "_");
                layers.put(key + "_issues", entry.getValue());
            }
            info.setLayers(layers);

            // Patterns detected (agent names)
            info.setPatterns(new ArrayList<>(issuesByAgent.keySet()));

            // All issues from all agents (keep for backward compatibility)
            info.setArchitecturalIssues(allIssues);
            info.setHasArchitecturalIssues(totalIssues > 0);

            // Full markdown report
            info.setFullMarkdownReport(markdownReport.toString());

            // Generate comprehensive recommendations
            List<String> recommendations = generateRecommendations(totalIssues, issuesByAgent, issuesBySeverity);
            info.setRecommendations(recommendations);

            log.info("✅ AI comprehensive analysis complete: filesAnalyzed={}, filesWithIssues={}, totalIssues={}, agents={}",
                    filesAnalyzed, filesWithIssues, totalIssues, issuesByAgent.keySet());

        } catch (Exception e) {
            log.error("❌ AI comprehensive analysis failed", e);
            info.setArchitecturalIssues(List.of("AI分析失败: " + e.getMessage()));
            info.setHasArchitecturalIssues(true);
            info.setRecommendations(List.of("❌ AI分析出现错误，建议检查系统配置或联系技术支持"));
            info.setFullMarkdownReport("# ❌ 分析失败\n\n分析过程中发生错误: " + e.getMessage());
        }

        return info;
    }

    /**
     * Generate comprehensive markdown report
     */
    private void generateMarkdownReport(StringBuilder md, List<ProjectFile> files, int filesAnalyzed,
                                        int filesWithIssues, int totalIssues,
                                        Map<String, Integer> issuesByAgent,
                                        Map<String, Integer> issuesBySeverity,
                                        Map<String, Integer> issuesByCategory,
                                        List<FileIssueDetail> fileIssueDetails) {

        // Executive Summary
        md.append("## 📊 执行摘要\n\n");
        md.append("**分析范围**:\n");
        md.append("- 项目文件总数: `").append(files.size()).append("`\n");
        md.append("- 已分析文件: `").append(filesAnalyzed).append("`\n");
        md.append("- 发现问题文件: `").append(filesWithIssues).append("`\n");
        md.append("- 问题总数: `").append(totalIssues).append("`").append("\n\n");

        if (totalIssues > 0) {
            // Severity distribution
            md.append("### 🔴 严重程度分布\n\n");
            for (Map.Entry<String, Integer> entry : issuesBySeverity.entrySet()) {
                String icon = getSeverityIcon(entry.getKey());
                md.append("- ").append(icon).append(" **").append(entry.getKey()).append("**: `")
                  .append(entry.getValue()).append("` 个问题\n");
            }
            md.append("\n");

            // Agent results
            md.append("### 🤖 各智能体检测结果\n\n");
            for (Map.Entry<String, Integer> entry : issuesByAgent.entrySet()) {
                String agentIcon = getAgentIcon(entry.getKey());
                md.append(agentIcon).append(" **").append(entry.getKey()).append("**: `")
                  .append(entry.getValue()).append("` 个问题\n");
            }
            md.append("\n");

            // Category breakdown
            if (!issuesByCategory.isEmpty()) {
                md.append("### 📁 问题分类统计\n\n");
                issuesByCategory.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .forEach(entry -> md.append("- **").append(entry.getKey()).append("**: `")
                        .append(entry.getValue()).append("` 个\n"));
                md.append("\n");
            }
        } else {
            md.append("✅ **未发现明显问题** - 代码质量良好！\n\n");
        }

        md.append("---\n\n");

        // File-by-file detailed analysis
        if (!fileIssueDetails.isEmpty()) {
            md.append("## 📁 文件详细分析\n\n");

            for (FileIssueDetail fileDetail : fileIssueDetails) {
                md.append("### 📄 ").append(fileDetail.getFileName()).append("\n\n");
                md.append("**路径**: `").append(fileDetail.getFilePath()).append("`  \n");
                md.append("**语言**: `").append(fileDetail.getLanguage()).append("`  \n");
                md.append("**问题数**: `").append(fileDetail.getTotalIssues()).append("`\n\n");

                // Group issues by agent
                Map<String, List<IssueDetail>> issuesByAgentForFile = fileDetail.getIssues().stream()
                    .collect(java.util.stream.Collectors.groupingBy(IssueDetail::getAgentName));

                for (Map.Entry<String, List<IssueDetail>> agentEntry : issuesByAgentForFile.entrySet()) {
                    md.append("#### ").append(getAgentIcon(agentEntry.getKey()))
                      .append(" ").append(agentEntry.getKey()).append("\n\n");

                    for (IssueDetail issue : agentEntry.getValue()) {
                        md.append(getSeverityIcon(issue.getSeverity())).append(" **")
                          .append(issue.getTitle()).append("**\n\n");
                        md.append("**类型**: ").append(issue.getCategory()).append("  \n");
                        md.append("**严重程度**: ").append(issue.getSeverity()).append("\n\n");
                        md.append("**描述**: ").append(issue.getDescription()).append("\n\n");

                        if (issue.getSuggestion() != null && !issue.getSuggestion().isEmpty()) {
                            md.append("**💡 建议**: ").append(issue.getSuggestion()).append("\n\n");
                        }

                        if (issue.getCodeSnippet() != null) {
                            md.append("**📍 代码位置**:\n```").append(getLanguageAlias(fileDetail.getLanguage()))
                              .append("\n").append(issue.getCodeSnippet()).append("\n```\n\n");
                        }
                    }
                }
                md.append("\n");
            }

            md.append("---\n\n");
        }

        // Recommendations section
        md.append("## 💡 改进建议\n\n");

        if (totalIssues > 0) {
            md.append("### 优先级修复顺序\n\n");

            // High severity first
            long highCount = issuesBySeverity.getOrDefault("HIGH", 0);
            long mediumCount = issuesBySeverity.getOrDefault("MEDIUM", 0);
            long lowCount = issuesBySeverity.getOrDefault("LOW", 0);

            if (highCount > 0) {
                md.append("1. **🔴 高优先级** - 立即修复 ").append(highCount).append(" 个高危问题\n");
                md.append("   - 安全漏洞和架构缺陷可能导致生产事故\n\n");
            }
            if (mediumCount > 0) {
                md.append("2. **🟡 中优先级** - 计划修复 ").append(mediumCount).append(" 个中等问题\n");
                md.append("   - 代码规范和性能优化建议\n\n");
            }
            if (lowCount > 0) {
                md.append("3. **🟢 低优先级** - 逐步改进 ").append(lowCount).append(" 个轻微问题\n");
                md.append("   - 代码风格和小优化建议\n\n");
            }

            // Agent-specific recommendations
            md.append("### 按智能体分类的建议\n\n");

            if (issuesByAgent.containsKey("SecurityAuditor")) {
                md.append("#### 🔒 安全审计 (SecurityAuditor)\n");
                md.append("发现 ").append(issuesByAgent.get("SecurityAuditor")).append(" 个安全问题\n");
                md.append("- 优先修复SQL注入、XSS等高危漏洞\n");
                md.append("- 添加输入验证和输出编码\n");
                md.append("- 实施最小权限原则\n\n");
            }

            if (issuesByAgent.containsKey("ArchitectureGuardian")) {
                md.append("#### 🏗️ 架构守护 (ArchitectureGuardian)\n");
                md.append("发现 ").append(issuesByAgent.get("ArchitectureGuardian")).append(" 个架构问题\n");
                md.append("- 重构过长的方法和类\n");
                md.append("- 遵循SOLID原则\n");
                md.append("- 减少类之间的耦合\n\n");
            }

            if (issuesByAgent.containsKey("CodeStandardsInspector")) {
                md.append("#### 📋 代码规范 (CodeStandardsInspector)\n");
                md.append("发现 ").append(issuesByAgent.get("CodeStandardsInspector")).append(" 个规范问题\n");
                md.append("- 统一命名规范\n");
                md.append("- 添加必要的注释和文档\n");
                md.append("- 优化代码格式\n\n");
            }

            if (issuesByAgent.containsKey("PerformanceOptimizer")) {
                md.append("#### ⚡ 性能优化 (PerformanceOptimizer)\n");
                md.append("发现 ").append(issuesByAgent.get("PerformanceOptimizer")).append(" 个性能问题\n");
                md.append("- 优化数据库查询\n");
                md.append("- 减少不必要的对象创建\n");
                md.append("- 使用缓存提高响应速度\n\n");
            }

            md.append("### 🔄 持续改进\n\n");
            md.append("1. 建议定期重新运行分析，跟踪改进进度\n");
            md.append("2. 将代码审查集成到CI/CD流程\n");
            md.append("3. 建立代码质量基线和监控\n");
        } else {
            md.append("✅ **代码质量优秀！**\n\n");
            md.append("继续保持良好的编码实践：\n");
            md.append("- 遵循设计模式和SOLID原则\n");
            md.append("- 保持代码简洁和可读性\n");
            md.append("- 定期进行代码审查\n");
            md.append("- 编写全面的单元测试\n");
        }

        md.append("\n---\n\n");
        md.append("*本报告由 AI 多智能体系统自动生成*  ");
        md.append("`").append(new java.util.Date()).append("`\n");
    }

    /**
     * Extract code snippet around a specific line
     */
    private String extractCodeSnippet(String code, Integer lineNumber, int contextLines) {
        if (code == null || lineNumber == null || lineNumber <= 0) {
            return null;
        }

        String[] lines = code.split("\n");
        int start = Math.max(0, lineNumber - contextLines - 1);
        int end = Math.min(lines.length, lineNumber + contextLines);

        StringBuilder snippet = new StringBuilder();
        for (int i = start; i < end; i++) {
            String prefix = (i == lineNumber - 1) ? ">>> " : "    ";
            snippet.append(prefix).append(lines[i]).append("\n");
        }

        return snippet.toString().trim();
    }

    /**
     * Get language alias for code highlighting
     */
    private String getLanguageAlias(String language) {
        if (language == null) return "text";
        return switch (language.toLowerCase()) {
            case "java" -> "java";
            case "javascript", "js" -> "javascript";
            case "typescript", "ts" -> "typescript";
            case "python", "py" -> "python";
            case "go" -> "go";
            case "rust" -> "rust";
            case "cpp", "c++" -> "cpp";
            case "c" -> "c";
            default -> "text";
        };
    }

    /**
     * Get severity icon
     */
    private String getSeverityIcon(String severity) {
        return switch (severity.toUpperCase()) {
            case "HIGH", "CRITICAL" -> "🔴";
            case "MEDIUM" -> "🟡";
            case "LOW" -> "🟢";
            default -> "⚪";
        };
    }

    /**
     * Get agent icon
     */
    private String getAgentIcon(String agentName) {
        return switch (agentName) {
            case "SecurityAuditor" -> "🔒";
            case "ArchitectureGuardian" -> "🏗️";
            case "CodeStandardsInspector" -> "📋";
            case "PerformanceOptimizer" -> "⚡";
            default -> "🤖";
        };
    }

    /**
     * Generate comprehensive recommendations
     */
    private List<String> generateRecommendations(int totalIssues, Map<String, Integer> issuesByAgent,
                                                  Map<String, Integer> issuesBySeverity) {
        List<String> recommendations = new ArrayList<>();

        if (totalIssues > 0) {
            recommendations.add("🤖 AI 多智能体综合分析完成，发现 " + totalIssues + " 个问题");

            // Agent-specific recommendations
            if (issuesByAgent.containsKey("SecurityAuditor")) {
                recommendations.add("🔒 安全问题: " + issuesByAgent.get("SecurityAuditor") + " 个 - 优先修复安全漏洞");
            }
            if (issuesByAgent.containsKey("ArchitectureGuardian")) {
                recommendations.add("🏗️ 架构问题: " + issuesByAgent.get("ArchitectureGuardian") + " 个 - 关注设计模式与SOLID原则");
            }
            if (issuesByAgent.containsKey("CodeStandardsInspector")) {
                recommendations.add("📋 代码规范: " + issuesByAgent.get("CodeStandardsInspector") + " 个 - 统一代码风格与命名");
            }
            if (issuesByAgent.containsKey("PerformanceOptimizer")) {
                recommendations.add("⚡ 性能问题: " + issuesByAgent.get("PerformanceOptimizer") + " 个 - 优化性能瓶颈");
            }

            // Severity-based recommendations
            int highSeverity = issuesBySeverity.getOrDefault("HIGH", 0) + issuesBySeverity.getOrDefault("CRITICAL", 0);
            if (highSeverity > 0) {
                recommendations.add("⚠️ 发现 " + highSeverity + " 个高风险问题，建议立即修复");
            }

            recommendations.add("💡 建议：按优先级逐项修复，每次修复后重新分析验证");
            recommendations.add("📄 完整分析报告请查看报告详情页");
        } else {
            recommendations.add("✅ AI 多智能体分析：代码质量良好");
            recommendations.add("🎉 继续保持良好的编码规范和架构设计");
        }

        return recommendations;
    }

    /**
     * Delete directory recursively
     */
    private void deleteDirectory(Path directory) throws Exception {
        if (!Files.exists(directory)) {
            return;
        }

        Files.walk(directory)
                .sorted((a, b) -> b.compareTo(a)) // Reverse order for deletion
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception e) {
                        log.warn("Failed to delete: {}", path, e);
                    }
                });
    }

    // Inner class for architecture analysis results

    @Data
    public static class ProjectArchitectureInfo {
        private String projectType;
        private Map<String, Integer> layers;
        private List<String> patterns;
        private List<String> architecturalIssues;
        private boolean hasArchitecturalIssues;
        private List<String> recommendations;

        // Enhanced detailed analysis data
        private List<FileIssueDetail> fileIssueDetails;
        private Map<String, List<AgentIssueSummary>> issuesByAgent;
        private Map<String, Integer> severityDistribution;
        private String fullMarkdownReport;
    }

    /**
     * Detailed issue information for a specific file
     */
    @Data
    public static class FileIssueDetail {
        private String fileName;
        private String filePath;
        private String language;
        private int totalIssues;
        private List<IssueDetail> issues;
    }

    /**
     * Individual issue detail
     */
    @Data
    public static class IssueDetail {
        private String agentName;
        private String severity;
        private String category;
        private String title;
        private String description;
        private String suggestion;
        private Integer lineNumber;
        private String codeSnippet;
    }

    /**
     * Agent issue summary
     */
    @Data
    public static class AgentIssueSummary {
        private String agentName;
        private int issueCount;
        private Map<String, Integer> severityBreakdown;
        private Map<String, Integer> categoryBreakdown;
        private List<String> topIssues;
    }
}
