package com.codereview.ai.domain.service;

import com.codereview.ai.domain.config.FileFilterConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Project File Scanner
 * Scans project directory for files to analyze
 *
 * @author Code Review AI Team
 */
@Service
@RequiredArgsConstructor
public class ProjectFileScanner {

    private static final Logger log = LoggerFactory.getLogger(ProjectFileScanner.class);

    private final FileFilterConfig filterConfig;

    /**
     * Scan project directory for eligible files
     *
     * @param projectRoot Root directory of the project
     * @return List of file metadata, prioritized for analysis
     */
    public List<ProjectFileMetadata> scanProject(Path projectRoot) {
        log.info("Starting project scan: {}", projectRoot);

        List<ProjectFileMetadata> files = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(projectRoot)) {
            paths.filter(Files::isRegularFile)
                 .filter(this::shouldInclude)
                 .map(path -> extractMetadata(path, projectRoot))
                 .filter(this::isValidSize)
                 .forEach(files::add);

        } catch (IOException e) {
            log.error("Error scanning project directory: {}", projectRoot, e);
        }

        List<ProjectFileMetadata> prioritized = prioritizeFiles(files);

        log.info("Project scan complete: {} files found", prioritized.size());
        log.debug("Files: {}", prioritized.stream()
            .map(ProjectFileMetadata::getRelativePath)
            .toList());

        return prioritized;
    }

    /**
     * Check if a file should be included in analysis
     */
    public boolean shouldInclude(Path path) {
        String fileName = path.getFileName().toString();
        String pathStr = path.toString().replace("\\", "/");

        // Check excluded directories
        for (String dir : filterConfig.getExcludedDirectories()) {
            if (pathStr.contains("/" + dir + "/") ||
                pathStr.startsWith(dir + "/") ||
                pathStr.contains("/" + dir + "/") ||
                pathStr.endsWith("/" + dir)) {
                log.trace("Excluding file in excluded directory: {}", path);
                return false;
            }
        }

        // Check excluded patterns
        for (String pattern : filterConfig.getExcludedPatterns()) {
            String wildcard = pattern.replace("*", "");
            if (fileName.endsWith(wildcard) || fileName.contains(wildcard)) {
                log.trace("Excluding file matching pattern: {} matches {}", path, pattern);
                return false;
            }
        }

        // Check included extensions
        boolean hasIncludedExtension = filterConfig.getIncludedExtensions().stream()
            .anyMatch(fileName::endsWith);

        if (!hasIncludedExtension) {
            log.trace("Excluding file with no included extension: {}", path);
            return false;
        }

        return true;
    }

    /**
     * Extract metadata from a file path
     *
     * @param path The absolute path to the file
     * @param projectRoot The root directory of the project (for relative path calculation)
     * @return ProjectFileMetadata with relative path computed from projectRoot
     */
    public ProjectFileMetadata extractMetadata(Path path, Path projectRoot) {
        String fileName = path.getFileName().toString();

        // Compute relative path from projectRoot
        Path relativePath;
        try {
            relativePath = projectRoot.relativize(path);
        } catch (IllegalArgumentException e) {
            // If path is not relative to projectRoot, use file name only
            log.warn("Path {} is not relative to projectRoot {}, using filename only", path, projectRoot);
            relativePath = Path.of(fileName);
        }
        String relativePathStr = relativePath.toString().replace("\\", "/");

        // Detect language
        String language = ProjectFileMetadata.detectLanguage(fileName);

        // Get file size
        long size = 0;
        try {
            size = Files.size(path);
        } catch (IOException e) {
            log.warn("Could not get file size: {}", path);
        }

        // Count lines
        int lineCount = 0;
        try {
            lineCount = (int) Files.lines(path).count();
        } catch (IOException e) {
            log.warn("Could not count lines: {}", path);
        }

        // Check if in src directory
        boolean isInSrcDir = relativePathStr.startsWith("src/") ||
                            relativePathStr.contains("/src/") ||
                            relativePathStr.startsWith("app/") ||
                            relativePathStr.contains("/app/");

        // Check if entry point
        boolean isEntryPoint = ProjectFileMetadata.isEntryPointFile(fileName);

        // Check if test file
        boolean isTestFile = ProjectFileMetadata.isTestFile(relativePathStr);

        // Check if config file
        boolean isConfigFile = ProjectFileMetadata.isConfigFile(fileName);

        return ProjectFileMetadata.builder()
            .filePath(path)
            .fileName(fileName)
            .relativePath(relativePathStr)
            .language(language)
            .size(size)
            .lineCount(lineCount)
            .isInSrcDir(isInSrcDir)
            .isEntryPoint(isEntryPoint)
            .isTestFile(isTestFile)
            .isConfigFile(isConfigFile)
            .build();
    }

    /**
     * Check if file size is valid
     */
    public boolean isValidSize(ProjectFileMetadata metadata) {
        // Check minimum size
        if (metadata.getSize() < filterConfig.getMinFileSizeBytes()) {
            log.trace("Excluding file below minimum size: {}", metadata.getRelativePath());
            return false;
        }

        // Check maximum size
        if (metadata.getSize() > filterConfig.getMaxFileSizeBytes()) {
            log.warn("Excluding file above maximum size: {} ({} bytes)",
                metadata.getRelativePath(), metadata.getSize());
            return false;
        }

        // Check line count
        if (metadata.getLineCount() > filterConfig.getMaxLinesPerFile()) {
            log.warn("Excluding file with too many lines: {} ({} lines)",
                metadata.getRelativePath(), metadata.getLineCount());
            return false;
        }

        return true;
    }

    /**
     * Prioritize files for analysis
     * Higher priority = analyze first
     */
    public List<ProjectFileMetadata> prioritizeFiles(List<ProjectFileMetadata> files) {
        return files.stream()
            .sorted(Comparator
                // Entry points first
                .comparing(ProjectFileMetadata::isEntryPoint).reversed()
                // Then src directory files
                .thenComparing(ProjectFileMetadata::isInSrcDir).reversed()
                // Then non-test files
                .thenComparing(ProjectFileMetadata::isTestFile)
                // Then non-config files
                .thenComparing(ProjectFileMetadata::isConfigFile)
                // Then by size (smaller first for faster feedback)
                .thenComparing(ProjectFileMetadata::getSize))
            .toList();
    }
}
