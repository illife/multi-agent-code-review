package com.codereview.ai.domain.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

/**
 * File Filter Configuration
 * Defines which files to include/exclude during project scanning
 *
 * @author Code Review AI Team
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "project.file-filter")
public class FileFilterConfig {

    /**
     * File extensions to include in analysis
     */
    private Set<String> includedExtensions = new HashSet<>(Set.of(
        ".java", ".py", ".js", ".ts", ".jsx", ".tsx",
        ".go", ".rs", ".c", ".cpp", ".h", ".cs",
        ".php", ".rb", ".swift", ".kt", ".scala",
        ".vue", ".svelte", ".astro"
    ));

    /**
     * Directory names to exclude from scanning
     */
    private Set<String> excludedDirectories = new HashSet<>(Set.of(
        "node_modules", ".git", "vendor", "build",
        "dist", "target", "out", "bin", ".gradle",
        ".idea", ".vscode", "coverage", "__pycache__",
        ".next", ".nuxt", "venv", "env", ".venv"
    ));

    /**
     * File patterns to exclude (wildcard support)
     */
    private Set<String> excludedPatterns = new HashSet<>(Set.of(
        "*.min.js", "*.min.css", "*.bundle.js",
        "*.bundle.css", "*.chunk.js", "*.chunk.css",
        "package-lock.json", "yarn.lock", "pnpm-lock.yaml",
        "*.pyc", "*.pyo", "*.class", "*.jar",
        "*.war", "*.ear", "*.dll", "*.so", "*.dylib"
    ));

    /**
     * Maximum file size in bytes (default: 10MB)
     */
    private long maxFileSizeBytes = 10 * 1024 * 1024;

    /**
     * Maximum lines per file (default: 5000)
     */
    private int maxLinesPerFile = 5000;

    /**
     * Minimum file size in bytes (skip empty files)
     */
    private long minFileSizeBytes = 10;

    @PostConstruct
    public void init() {
        log.info("====================================");
        log.info("✅ FileFilterConfig Initialized");
        log.info("====================================");
        log.info("Included Extensions: {}", includedExtensions);
        log.info("Excluded Directories: {}", excludedDirectories);
        log.info("Excluded Patterns: {}", excludedPatterns);
        log.info("Max File Size: {} bytes ({} MB)", maxFileSizeBytes, maxFileSizeBytes / (1024 * 1024));
        log.info("Max Lines Per File: {}", maxLinesPerFile);
        log.info("====================================");
    }

    // Manual getters since Lombok is not working
    public Set<String> getIncludedExtensions() { return includedExtensions; }
    public Set<String> getExcludedDirectories() { return excludedDirectories; }
    public Set<String> getExcludedPatterns() { return excludedPatterns; }
    public long getMaxFileSizeBytes() { return maxFileSizeBytes; }
    public int getMaxLinesPerFile() { return maxLinesPerFile; }
    public long getMinFileSizeBytes() { return minFileSizeBytes; }
}
