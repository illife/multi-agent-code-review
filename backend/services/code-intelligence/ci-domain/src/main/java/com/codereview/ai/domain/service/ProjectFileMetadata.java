package com.codereview.ai.domain.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;
import java.util.Set;

/**
 * Metadata for a project file
 * Used during file scanning and prioritization
 *
 * @author Code Review AI Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectFileMetadata {
    private Path filePath;
    private String fileName;
    private String relativePath;
    private String language;
    private long size;
    private int lineCount;
    private boolean isInSrcDir;
    private boolean isEntryPoint;
    private boolean isTestFile;
    private boolean isConfigFile;

    // Manual getters since Lombok is not working
    public Path getFilePath() { return filePath; }
    public String getFileName() { return fileName; }
    public String getRelativePath() { return relativePath; }
    public String getLanguage() { return language; }
    public long getSize() { return size; }
    public int getLineCount() { return lineCount; }
    public boolean isInSrcDir() { return isInSrcDir; }
    public boolean isEntryPoint() { return isEntryPoint; }
    public boolean isTestFile() { return isTestFile; }
    public boolean isConfigFile() { return isConfigFile; }

    public static ProjectFileMetadataBuilder builder() { return new ProjectFileMetadataBuilder(); }

    public static class ProjectFileMetadataBuilder {
        private Path filePath;
        private String fileName;
        private String relativePath;
        private String language;
        private long size;
        private int lineCount;
        private boolean isInSrcDir;
        private boolean isEntryPoint;
        private boolean isTestFile;
        private boolean isConfigFile;

        public ProjectFileMetadataBuilder filePath(Path filePath) { this.filePath = filePath; return this; }
        public ProjectFileMetadataBuilder fileName(String fileName) { this.fileName = fileName; return this; }
        public ProjectFileMetadataBuilder relativePath(String relativePath) { this.relativePath = relativePath; return this; }
        public ProjectFileMetadataBuilder language(String language) { this.language = language; return this; }
        public ProjectFileMetadataBuilder size(long size) { this.size = size; return this; }
        public ProjectFileMetadataBuilder lineCount(int lineCount) { this.lineCount = lineCount; return this; }
        public ProjectFileMetadataBuilder isInSrcDir(boolean isInSrcDir) { this.isInSrcDir = isInSrcDir; return this; }
        public ProjectFileMetadataBuilder isEntryPoint(boolean isEntryPoint) { this.isEntryPoint = isEntryPoint; return this; }
        public ProjectFileMetadataBuilder isTestFile(boolean isTestFile) { this.isTestFile = isTestFile; return this; }
        public ProjectFileMetadataBuilder isConfigFile(boolean isConfigFile) { this.isConfigFile = isConfigFile; return this; }

        public ProjectFileMetadata build() {
            return new ProjectFileMetadata(filePath, fileName, relativePath, language, size, lineCount,
                isInSrcDir, isEntryPoint, isTestFile, isConfigFile);
        }
    }

    // Entry point patterns
    private static final Set<String> ENTRY_POINT_PATTERNS = Set.of(
        "index.js", "index.ts", "index.jsx", "index.tsx",
        "main.js", "main.ts", "main.go", "main.py",
        "app.js", "app.ts", "App.jsx", "App.tsx",
        "server.js", "server.ts", "index.php",
        "Application.java", "Main.java", "Program.cs"
    );

    // Test file patterns
    private static final Set<String> TEST_PATTERNS = Set.of(
        ".test.", ".spec.", "_test.", "_spec.",
        "/test/", "/tests/", "/__tests__/",
        "Test.java", "Tests.java", "Test.cs"
    );

    // Config file patterns
    private static final Set<String> CONFIG_PATTERNS = Set.of(
        ".config.", "config.", ".conf.",
        "webpack.config", "vite.config", "tsconfig",
        "package.json", "pom.xml", "build.gradle",
        "Dockerfile", "docker-compose"
    );

    /**
     * Check if file is an entry point
     */
    public static boolean isEntryPointFile(String fileName) {
        return ENTRY_POINT_PATTERNS.stream()
            .anyMatch(pattern -> fileName.equals(pattern) || fileName.endsWith(pattern));
    }

    /**
     * Check if file is a test file
     */
    public static boolean isTestFile(String relativePath) {
        return TEST_PATTERNS.stream()
            .anyMatch(relativePath::contains);
    }

    /**
     * Check if file is a config file
     */
    public static boolean isConfigFile(String fileName) {
        return CONFIG_PATTERNS.stream()
            .anyMatch(pattern -> fileName.contains(pattern));
    }

    /**
     * Detect language from file extension
     */
    public static String detectLanguage(String fileName) {
        String extension = "";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            extension = fileName.substring(lastDot).toLowerCase();
        }

        return switch (extension) {
            case ".java" -> "JAVA";
            case ".py" -> "PYTHON";
            case ".js" -> "JAVASCRIPT";
            case ".jsx" -> "JAVASCRIPT";
            case ".ts" -> "TYPESCRIPT";
            case ".tsx" -> "TYPESCRIPT";
            case ".go" -> "GO";
            case ".rs" -> "RUST";
            case ".c" -> "C";
            case ".cpp" -> "CPP";
            case ".h" -> "C";
            case ".hpp" -> "CPP";
            case ".cs" -> "CSHARP";
            case ".php" -> "PHP";
            case ".rb" -> "RUBY";
            case ".swift" -> "SWIFT";
            case ".kt" -> "KOTLIN";
            case ".scala" -> "SCALA";
            case ".vue" -> "VUE";
            case ".svelte" -> "SVELTE";
            case ".astro" -> "ASTRO";
            default -> "UNKNOWN";
        };
    }
}
