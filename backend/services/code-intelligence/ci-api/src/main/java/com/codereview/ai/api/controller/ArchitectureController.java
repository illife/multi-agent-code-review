package com.codereview.ai.api.controller;

import com.codereview.ai.domain.service.ArchitectureAnalysisService;
import com.think.platform.shared.common.result.Result;
import com.codereview.ai.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Architecture Analysis Controller
 * Provides endpoints for analyzing project architecture and dependencies
 *
 * @author Code Review AI Team
 */
@Slf4j
@RestController
@RequestMapping("/api/architecture")
@RequiredArgsConstructor
public class ArchitectureController {

    private final ArchitectureAnalysisService architectureAnalysisService;
    private final SecurityUtils securityUtils;

    /**
     * Analyze current project architecture
     *
     * @param request HTTP request
     * @return Architecture analysis report
     */
    @GetMapping("/analyze")
    public Result<ArchitectureAnalysisService.ArchitectureReport> analyzeArchitecture(
            HttpServletRequest request) {
        try {
            Long userId = securityUtils.getCurrentUserId(request);
            log.info("Architecture analysis requested by user: {}", userId);

            // Analyze the backend project
            Path projectRoot = Paths.get("C:/Users/HP/Desktop/think/backend");
            ArchitectureAnalysisService.ArchitectureReport report = architectureAnalysisService.analyzeArchitecture(projectRoot);

            log.info("Architecture analysis complete: {} modules, {} circular dependencies",
                    report.getModules().size(), report.getCircularDependencies().size());

            return Result.success(report);

        } catch (IOException e) {
            log.error("Failed to analyze architecture", e);
            return Result.error("Analysis failed: " + e.getMessage());
        }
    }

    /**
     * Get dependency graph as JSON
     *
     * @param request HTTP request
     * @return Dependency graph
     */
    @GetMapping("/dependency-graph")
    public Result<Map<String, Object>> getDependencyGraph(HttpServletRequest request) {
        try {
            Long userId = securityUtils.getCurrentUserId(request);
            log.info("Dependency graph requested by user: {}", userId);

            Path projectRoot = Paths.get("C:/Users/HP/Desktop/think/backend");
            ArchitectureAnalysisService.ArchitectureReport report = architectureAnalysisService.analyzeArchitecture(projectRoot);

            Map<String, Object> response = new HashMap<>();
            response.put("nodes", report.getModules());
            response.put("links", buildLinks(report.getDependencyGraph()));
            response.put("circularDependencies", report.getCircularDependencies());

            return Result.success(response);

        } catch (IOException e) {
            log.error("Failed to get dependency graph", e);
            return Result.error("Failed to generate graph: " + e.getMessage());
        }
    }

    /**
     * Get infrastructure usage summary
     *
     * @param request HTTP request
     * @return Infrastructure usage
     */
    @GetMapping("/infrastructure")
    public Result<ArchitectureAnalysisService.InfrastructureUsage> getInfrastructureUsage(
            HttpServletRequest request) {
        try {
            Long userId = securityUtils.getCurrentUserId(request);
            log.info("Infrastructure usage requested by user: {}", userId);

            Path projectRoot = Paths.get("C:/Users/HP/Desktop/think/backend");
            ArchitectureAnalysisService.ArchitectureReport report = architectureAnalysisService.analyzeArchitecture(projectRoot);

            return Result.success(report.getInfrastructureUsage());

        } catch (IOException e) {
            log.error("Failed to get infrastructure usage", e);
            return Result.error("Failed to analyze infrastructure: " + e.getMessage());
        }
    }

    /**
     * Get architectural recommendations
     *
     * @param request HTTP request
     * @return Recommendations
     */
    @GetMapping("/recommendations")
    public Result<Map<String, Object>> getRecommendations(HttpServletRequest request) {
        try {
            Long userId = securityUtils.getCurrentUserId(request);
            log.info("Architecture recommendations requested by user: {}", userId);

            Path projectRoot = Paths.get("C:/Users/HP/Desktop/think/backend");
            ArchitectureAnalysisService.ArchitectureReport report = architectureAnalysisService.analyzeArchitecture(projectRoot);

            Map<String, Object> response = new HashMap<>();
            response.put("recommendations", report.getRecommendations());
            response.put("circularDependencies", report.getCircularDependencies());
            response.put("moduleCount", report.getModules().size());
            response.put("hasIssues", !report.getCircularDependencies().isEmpty());

            return Result.success(response);

        } catch (IOException e) {
            log.error("Failed to get recommendations", e);
            return Result.error("Failed to generate recommendations: " + e.getMessage());
        }
    }

    /**
     * Build dependency links for visualization
     */
    private Map<String, Object> buildLinks(Map<String, java.util.Set<String>> dependencyGraph) {
        Map<String, Object> links = new HashMap<>();
        int linkId = 0;

        for (Map.Entry<String, java.util.Set<String>> entry : dependencyGraph.entrySet()) {
            String source = entry.getKey();
            for (String target : entry.getValue()) {
                Map<String, Object> link = new HashMap<>();
                link.put("id", linkId++);
                link.put("source", source);
                link.put("target", target);
                link.put("type", "depends-on");
                links.put("link-" + linkId, link);
            }
        }

        return links;
    }
}
