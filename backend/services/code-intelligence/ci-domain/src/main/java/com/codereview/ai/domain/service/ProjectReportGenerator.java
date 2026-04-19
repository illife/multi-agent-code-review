package com.codereview.ai.domain.service;

import com.codereview.ai.domain.consumer.ProjectAnalysisConsumer.ProjectArchitectureInfo;
import com.codereview.ai.domain.model.CodeIssue;
import com.codereview.ai.domain.model.Project;
import com.codereview.ai.domain.model.ProjectFile;
import com.codereview.ai.domain.model.ProjectReport;
import com.codereview.ai.domain.repository.ProjectFileRepository;
import com.codereview.ai.domain.repository.ProjectReportRepository;
import com.codereview.ai.domain.repository.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Project Report Generator
 * Generates comprehensive analysis reports for projects
 *
 * @author Code Review AI Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectReportGenerator {

    private final ProjectRepository projectRepository;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectReportRepository projectReportRepository;
    private final ObjectMapper objectMapper;

    // TODO: Inject CodeIssueRepository when needed
    // private final CodeIssueRepository codeIssueRepository;

    /**
     * Generate a comprehensive project report
     *
     * @param projectId Project ID
     * @return Generated report
     */
    @Transactional
    public ProjectReport generateReport(Long projectId) {
        log.info("Generating project report: projectId={}", projectId);

        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        List<ProjectFile> files = projectFileRepository.findByProjectId(projectId);

        // TODO: Collect all issues from associated CodeReviews
        // List<Long> reviewIds = files.stream()
        //     .map(ProjectFile::getReviewId)
        //     .filter(Objects::nonNull)
        //     .collect(Collectors.toList());
        // List<CodeIssue> allIssues = codeIssueRepository.findByReviewIdIn(reviewIds);

        // For now, use the project's total issues count
        int totalIssues = project.getTotalIssues() != null ? project.getTotalIssues() : 0;

        ProjectReport report = ProjectReport.builder()
            .projectId(projectId)
            .summary(generateSummary(project, totalIssues, files))
            .overallScore(calculateScore(totalIssues, files))
            .riskLevel(determineRisk(totalIssues))
            .metrics(serializeMetrics(calculateMetrics(totalIssues, files)))
            .recommendations(generateRecommendations(totalIssues, files))
            .fileStatistics(serializeFileStats(generateFileStats(files)))
            .build();

        // Check if report already exists, update it instead of creating duplicate
        ProjectReport saved;
        if (projectReportRepository.existsByProjectId(projectId)) {
            log.info("Updating existing report for project: {}", projectId);
            ProjectReport existing = projectReportRepository.findByProjectId(projectId).orElseThrow();
            existing.setSummary(report.getSummary());
            existing.setOverallScore(report.getOverallScore());
            existing.setRiskLevel(report.getRiskLevel());
            existing.setMetrics(report.getMetrics());
            existing.setRecommendations(report.getRecommendations());
            existing.setFileStatistics(report.getFileStatistics());
            saved = projectReportRepository.save(existing);
        } else {
            log.info("Creating new report for project: {}", projectId);
            saved = projectReportRepository.save(report);
        }

        log.info("Project report generated: projectId={}, overallScore={}, riskLevel={}",
            projectId, saved.getOverallScore(), saved.getRiskLevel());

        return saved;
    }

    /**
     * Generate a comprehensive project report with architecture analysis
     *
     * @param projectId Project ID
     * @param architectureInfo Architecture analysis information
     * @return Generated report
     */
    @Transactional
    public ProjectReport generateReport(Long projectId, ProjectArchitectureInfo architectureInfo) {
        log.info("Generating project report with architecture: projectId={}", projectId);

        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        List<ProjectFile> files = projectFileRepository.findByProjectId(projectId);

        int totalIssues = project.getTotalIssues() != null ? project.getTotalIssues() : 0;

        // Include architecture info in metrics
        Map<String, Object> metrics = calculateMetrics(totalIssues, files);
        if (architectureInfo != null) {
            metrics.put("projectType", architectureInfo.getProjectType());
            metrics.put("layers", architectureInfo.getLayers());
            metrics.put("patterns", architectureInfo.getPatterns());
            metrics.put("hasArchitecturalIssues", architectureInfo.isHasArchitecturalIssues());
            metrics.put("architecturalIssueCount", architectureInfo.getArchitecturalIssues() != null ?
                    architectureInfo.getArchitecturalIssues().size() : 0);
        }

        // Combine architecture recommendations with code quality recommendations
        List<String> allRecommendations = new ArrayList<>();
        allRecommendations.addAll(parseRecommendationsToList(generateRecommendations(totalIssues, files)));
        if (architectureInfo != null && architectureInfo.getRecommendations() != null) {
            allRecommendations.addAll(architectureInfo.getRecommendations());
        }

        ProjectReport report = ProjectReport.builder()
            .projectId(projectId)
            .summary(generateSummary(project, totalIssues, files, architectureInfo))
            .overallScore(calculateScore(totalIssues, files, architectureInfo))
            .riskLevel(determineRisk(totalIssues, architectureInfo))
            .metrics(serializeMetrics(metrics))
            .recommendations(String.join("\n\n", allRecommendations))
            .fileStatistics(serializeFileStats(generateFileStats(files)))
            .build();

        // Check if report already exists, update it instead of creating duplicate
        ProjectReport saved;
        if (projectReportRepository.existsByProjectId(projectId)) {
            log.info("Updating existing report for project: {}", projectId);
            ProjectReport existing = projectReportRepository.findByProjectId(projectId).orElseThrow();
            // Update all fields
            existing.setSummary(report.getSummary());
            existing.setOverallScore(report.getOverallScore());
            existing.setRiskLevel(report.getRiskLevel());
            existing.setMetrics(report.getMetrics());
            existing.setRecommendations(report.getRecommendations());
            existing.setFileStatistics(report.getFileStatistics());
            saved = projectReportRepository.save(existing);
        } else {
            log.info("Creating new report for project: {}", projectId);
            saved = projectReportRepository.save(report);
        }

        log.info("Project report generated with architecture: projectId={}, overallScore={}, riskLevel={}",
            projectId, saved.getOverallScore(), saved.getRiskLevel());

        return saved;
    }

    /**
     * Calculate overall score (0-100)
     */
    private int calculateScore(int totalIssues, List<ProjectFile> files) {
        if (files.isEmpty()) {
            return 100;
        }

        int totalLoc = calculateTotalLOC(files);
        if (totalLoc == 0) {
            return 100;
        }

        // Base score 100, deduct based on issues
        int deduction = totalIssues * 2; // 2 points per issue

        // Normalize by project size (larger projects get more leniency)
        double sizeFactor = Math.min(totalLoc / 10000.0, 2.0);
        int adjustedDeduction = (int) (deduction / sizeFactor);

        return Math.max(0, 100 - adjustedDeduction);
    }

    /**
     * Calculate overall score with architecture info
     */
    private int calculateScore(int totalIssues, List<ProjectFile> files, ProjectArchitectureInfo architectureInfo) {
        int baseScore = calculateScore(totalIssues, files);

        // Adjust score based on architectural issues
        if (architectureInfo != null && architectureInfo.isHasArchitecturalIssues()) {
            int issueCount = architectureInfo.getArchitecturalIssues().size();
            baseScore = Math.max(0, baseScore - (issueCount * 5)); // Deduct 5 points per architectural issue
        }

        return baseScore;
    }

    /**
     * Determine risk level
     */
    private String determineRisk(int totalIssues) {
        // TODO: Use actual issue severity distribution
        if (totalIssues > 50) {
            return "CRITICAL";
        } else if (totalIssues > 20) {
            return "HIGH";
        } else if (totalIssues > 5) {
            return "MEDIUM";
        }
        return "LOW";
    }

    /**
     * Determine risk level with architecture info
     */
    private String determineRisk(int totalIssues, ProjectArchitectureInfo architectureInfo) {
        String baseRisk = determineRisk(totalIssues);

        // Upgrade risk level if there are architectural issues
        if (architectureInfo != null && architectureInfo.isHasArchitecturalIssues()) {
            if (baseRisk.equals("LOW")) {
                return "MEDIUM";
            } else if (baseRisk.equals("MEDIUM")) {
                return "HIGH";
            }
        }

        return baseRisk;
    }

    /**
     * Generate summary text
     */
    private String generateSummary(Project project, int totalIssues, List<ProjectFile> files) {
        int analyzedFiles = project.getAnalyzedFiles() != null ? project.getAnalyzedFiles() : 0;
        int totalFiles = project.getTotalFiles() != null ? project.getTotalFiles() : files.size();

        StringBuilder summary = new StringBuilder();
        summary.append("Project '").append(project.getProjectName()).append("' analysis complete.\n");
        summary.append("Analyzed ").append(analyzedFiles).append(" of ").append(totalFiles).append(" files.\n");
        summary.append("Found ").append(totalIssues).append(" total issues.\n");

        if (totalIssues > 0) {
            summary.append("\nIssue severity breakdown:\n");
            summary.append("- Critical: 0 (TBD)\n");
            summary.append("- High: 0 (TBD)\n");
            summary.append("- Medium: 0 (TBD)\n");
            summary.append("- Low: 0 (TBD)\n");
        }

        int loc = calculateTotalLOC(files);
        if (loc > 0) {
            double issuesPerKloc = (totalIssues * 1000.0) / loc;
            summary.append(String.format("\nCode quality: %.1f issues per 1000 lines of code.\n", issuesPerKloc));
        }

        return summary.toString();
    }

    /**
     * Generate summary text with architecture info
     */
    private String generateSummary(Project project, int totalIssues, List<ProjectFile> files,
                                   ProjectArchitectureInfo architectureInfo) {
        StringBuilder summary = new StringBuilder();
        summary.append(generateSummary(project, totalIssues, files));

        // Add architecture information
        if (architectureInfo != null) {
            summary.append("\n\n=== Architecture Analysis ===\n");
            summary.append("Project Type: ").append(architectureInfo.getProjectType()).append("\n");

            if (architectureInfo.getPatterns() != null && !architectureInfo.getPatterns().isEmpty()) {
                summary.append("Detected Patterns: ").append(String.join(", ", architectureInfo.getPatterns())).append("\n");
            }

            if (architectureInfo.getLayers() != null && !architectureInfo.getLayers().isEmpty()) {
                summary.append("Layer Structure:\n");
                architectureInfo.getLayers().forEach((layer, count) ->
                    summary.append("  - ").append(layer).append(": ").append(count).append(" files\n")
                );
            }

            if (architectureInfo.isHasArchitecturalIssues()) {
                summary.append("\nArchitectural Issues: ").append(architectureInfo.getArchitecturalIssues().size()).append("\n");
                architectureInfo.getArchitecturalIssues().forEach(issue ->
                    summary.append("  • ").append(issue).append("\n")
                );
            } else {
                summary.append("\n✅ No architectural issues detected\n");
            }
        }

        return summary.toString();
    }

    /**
     * Calculate metrics
     */
    private Map<String, Object> calculateMetrics(int totalIssues, List<ProjectFile> files) {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("totalIssues", totalIssues);
        metrics.put("criticalIssues", 0); // TODO: Calculate from actual issues
        metrics.put("highIssues", 0);
        metrics.put("mediumIssues", 0);
        metrics.put("lowIssues", 0);
        metrics.put("filesAnalyzed", files.size());
        metrics.put("totalLinesOfCode", calculateTotalLOC(files));
        metrics.put("totalFiles", files.size());

        int loc = calculateTotalLOC(files);
        if (loc > 0) {
            double issuesPerKloc = (totalIssues * 1000.0) / loc;
            metrics.put("issuesPerKLOC", Math.round(issuesPerKloc * 10.0) / 10.0);
        } else {
            metrics.put("issuesPerKLOC", 0.0);
        }

        // Language distribution
        Map<String, Long> languageDistribution = files.stream()
            .filter(f -> f.getLanguage() != null)
            .collect(Collectors.groupingBy(ProjectFile::getLanguage, Collectors.counting()));
        metrics.put("languageDistribution", languageDistribution);

        // Top file types with most issues (TODO: when issues are linked)
        metrics.put("topCategories", List.of("Code Style", "Security", "Performance"));

        return metrics;
    }

    /**
     * Generate file statistics
     */
    private Map<String, Object> generateFileStats(List<ProjectFile> files) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalFiles", files.size());
        stats.put("totalSize", files.stream().mapToLong(f -> f.getFileSize() != null ? f.getFileSize() : 0).sum());
        stats.put("totalLines", calculateTotalLOC(files));

        // File with most issues (TODO: when issues are linked)
        stats.put("fileWithMostIssues", "TBD");
        stats.put("mostProblematicFile", "TBD");

        // Language breakdown
        Map<String, Long> languageCount = files.stream()
            .filter(f -> f.getLanguage() != null)
            .collect(Collectors.groupingBy(ProjectFile::getLanguage, Collectors.counting()));
        stats.put("languageBreakdown", languageCount);

        // Average file size
        long avgSize = files.stream().mapToLong(f -> f.getFileSize() != null ? f.getFileSize() : 0).sum()
            / Math.max(files.size(), 1);
        stats.put("averageFileSize", avgSize);

        return stats;
    }

    /**
     * Generate recommendations
     */
    private String generateRecommendations(int totalIssues, List<ProjectFile> files) {
        StringBuilder recommendations = new StringBuilder();

        if (totalIssues == 0) {
            recommendations.append("✅ Great job! No issues found in the analyzed files.\n");
            recommendations.append("\nSuggestions:\n");
            recommendations.append("- Consider adding more comprehensive test coverage\n");
            recommendations.append("- Review code documentation completeness\n");
            recommendations.append("- Check for potential security vulnerabilities\n");
        } else {
            recommendations.append("⚠️ Issues found that require attention:\n\n");

            if (totalIssues > 20) {
                recommendations.append("1. **High Priority**: Address critical and high-severity issues first.\n");
                recommendations.append("2. Focus on security-related issues to protect your application.\n");
            } else {
                recommendations.append("1. Review medium and low severity issues.\n");
            }

            recommendations.append("2. Consider implementing code quality tools (ESLint, Prettier, etc.)\n");
            recommendations.append("3. Add unit tests for critical functionality\n");
            recommendations.append("4. Review dependency vulnerabilities\n");
        }

        return recommendations.toString();
    }

    /**
     * Parse recommendations string to list
     */
    private List<String> parseRecommendationsToList(String recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(recommendations.split("\n"))
                .filter(line -> !line.trim().isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Calculate total lines of code
     */
    private int calculateTotalLOC(List<ProjectFile> files) {
        return files.stream()
            .mapToInt(f -> f.getLineCount() != null ? f.getLineCount() : 0)
            .sum();
    }

    /**
     * Serialize metrics to JSON string
     */
    private String serializeMetrics(Map<String, Object> metrics) {
        try {
            return objectMapper.writeValueAsString(metrics);
        } catch (Exception e) {
            log.error("Failed to serialize metrics", e);
            return "{}";
        }
    }

    /**
     * Serialize file stats to JSON string
     */
    private String serializeFileStats(Map<String, Object> stats) {
        try {
            return objectMapper.writeValueAsString(stats);
        } catch (Exception e) {
            log.error("Failed to serialize file stats", e);
            return "{}";
        }
    }
}
