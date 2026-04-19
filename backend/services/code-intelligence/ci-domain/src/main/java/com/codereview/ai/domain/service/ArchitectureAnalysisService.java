package com.codereview.ai.domain.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Architecture Analysis Service
 * Analyzes project architecture and dependencies between modules
 *
 * @author Code Review AI Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchitectureAnalysisService {

    /**
     * Analyze the complete project architecture
     *
     * @param projectRoot Root directory of the project
     * @return Architecture report
     */
    public ArchitectureReport analyzeArchitecture(Path projectRoot) throws IOException {
        log.info("Starting architecture analysis: {}", projectRoot);

        ArchitectureReport report = new ArchitectureReport();
        report.setProjectRoot(projectRoot.toString());
        report.setAnalysisTime(new Date());

        // 1. Detect modules
        List<ModuleInfo> modules = detectModules(projectRoot);
        report.setModules(modules);
        log.info("Detected {} modules", modules.size());

        // 2. Analyze dependencies between modules
        Map<String, Set<String>> dependencyGraph = buildDependencyGraph(projectRoot, modules);
        report.setDependencyGraph(dependencyGraph);

        // 3. Detect circular dependencies
        List<CircularDependency> circularDeps = detectCircularDependencies(dependencyGraph);
        report.setCircularDependencies(circularDeps);
        log.info("Detected {} circular dependencies", circularDeps.size());

        // 4. Analyze external dependencies
        Map<String, Set<String>> externalDependencies = analyzeExternalDependencies(projectRoot, modules);
        report.setExternalDependencies(externalDependencies);

        // 5. Analyze infrastructure dependencies
        InfrastructureUsage infrastructure = analyzeInfrastructureUsage(projectRoot, modules);
        report.setInfrastructureUsage(infrastructure);

        // 6. Generate architecture layer analysis
        List<LayerInfo> layers = analyzeLayers(modules);
        report.setLayers(layers);

        // 7. Generate recommendations
        List<String> recommendations = generateRecommendations(report);
        report.setRecommendations(recommendations);

        log.info("Architecture analysis complete: {} modules, {} circular dependencies, {} recommendations",
                modules.size(), circularDeps.size(), recommendations.size());

        return report;
    }

    /**
     * Detect all modules in the project
     */
    private List<ModuleInfo> detectModules(Path projectRoot) throws IOException {
        List<ModuleInfo> modules = new ArrayList<>();

        // Check if it's a Maven multi-module project
        Path pomXml = projectRoot.resolve("pom.xml");
        if (Files.exists(pomXml)) {
            String pomContent = Files.readString(pomXml);
            List<String> moduleNames = extractMavenModules(pomContent);

            for (String moduleName : moduleNames) {
                Path modulePath = projectRoot.resolve(moduleName);
                if (Files.exists(modulePath)) {
                    ModuleInfo info = new ModuleInfo();
                    info.setName(moduleName);
                    info.setPath(modulePath.toString());

                    // Extract module info from pom.xml
                    Path modulePom = modulePath.resolve("pom.xml");
                    if (Files.exists(modulePom)) {
                        String modulePomContent = Files.readString(modulePom);
                        info.setArtifactId(extractTag(modulePomContent, "artifactId"));
                        info.setGroupId(extractTag(modulePomContent, "groupId"));
                        info.setVersion(extractTag(modulePomContent, "version"));
                        info.setDescription(extractTag(modulePomContent, "description"));

                        // Determine module type
                        info.setType(determineModuleType(modulePath));
                    }

                    // Count Java files
                    info.setJavaFileCount(countJavaFiles(modulePath));

                    modules.add(info);
                }
            }
        }

        return modules;
    }

    /**
     * Build dependency graph between modules
     */
    private Map<String, Set<String>> buildDependencyGraph(Path projectRoot, List<ModuleInfo> modules) throws IOException {
        Map<String, Set<String>> graph = new HashMap<>();

        for (ModuleInfo module : modules) {
            Set<String> dependencies = new HashSet<>();
            Path modulePom = Path.of(module.getPath()).resolve("pom.xml");

            if (Files.exists(modulePom)) {
                String pomContent = Files.readString(modulePom);
                dependencies.addAll(extractDependencies(pomContent));
            }

            // Filter to only include project modules
            Set<String> internalDependencies = dependencies.stream()
                    .filter(dep -> modules.stream()
                            .anyMatch(m -> dep.equals(m.getArtifactId()) || dep.equals(m.getName())))
                    .collect(Collectors.toSet());

            graph.put(module.getName(), internalDependencies);
        }

        return graph;
    }

    /**
     * Detect circular dependencies using DFS
     */
    private List<CircularDependency> detectCircularDependencies(Map<String, Set<String>> graph) {
        List<CircularDependency> circularDeps = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        List<String> path = new ArrayList<>();

        for (String module : graph.keySet()) {
            if (!visited.contains(module)) {
                detectCyclesDFS(module, graph, visited, recursionStack, path, circularDeps);
            }
        }

        return circularDeps;
    }

    private void detectCyclesDFS(String module, Map<String, Set<String>> graph,
                                  Set<String> visited, Set<String> recursionStack,
                                  List<String> path, List<CircularDependency> circularDeps) {
        visited.add(module);
        recursionStack.add(module);
        path.add(module);

        Set<String> dependencies = graph.getOrDefault(module, Collections.emptySet());
        for (String dep : dependencies) {
            if (!visited.contains(dep)) {
                detectCyclesDFS(dep, graph, visited, recursionStack, path, circularDeps);
            } else if (recursionStack.contains(dep)) {
                // Found a cycle
                int cycleStart = path.indexOf(dep);
                List<String> cycle = new ArrayList<>(path.subList(cycleStart, path.size()));
                cycle.add(dep);

                CircularDependency circularDep = new CircularDependency();
                circularDep.setCycle(cycle);
                circularDep.setDescription("Circular dependency detected: " + String.join(" -> ", cycle));
                circularDeps.add(circularDep);
            }
        }

        path.remove(path.size() - 1);
        recursionStack.remove(module);
    }

    /**
     * Analyze external dependencies (third-party libraries)
     */
    private Map<String, Set<String>> analyzeExternalDependencies(Path projectRoot, List<ModuleInfo> modules) throws IOException {
        Map<String, Set<String>> externalDeps = new HashMap<>();

        for (ModuleInfo module : modules) {
            Path modulePom = Path.of(module.getPath()).resolve("pom.xml");
            if (Files.exists(modulePom)) {
                String pomContent = Files.readString(modulePom);
                Set<String> dependencies = extractDependencies(pomContent);

                // Filter external dependencies (not project modules)
                Set<String> external = dependencies.stream()
                        .filter(dep -> modules.stream()
                                .noneMatch(m -> dep.equals(m.getArtifactId()) || dep.equals(m.getName())))
                        .collect(Collectors.toSet());

                externalDeps.put(module.getName(), external);
            }
        }

        return externalDeps;
    }

    /**
     * Analyze infrastructure usage (databases, message queues, caches)
     */
    private InfrastructureUsage analyzeInfrastructureUsage(Path projectRoot, List<ModuleInfo> modules) throws IOException {
        InfrastructureUsage usage = new InfrastructureUsage();

        for (ModuleInfo module : modules) {
            Path modulePath = Path.of(module.getPath());
            Path srcMainJava = modulePath.resolve("src/main/java");

            if (Files.exists(srcMainJava)) {
                // Scan Java files for infrastructure usage
                Files.walk(srcMainJava)
                        .filter(p -> p.toString().endsWith(".java"))
                        .forEach(javaFile -> {
                            try {
                                String content = Files.readString(javaFile);

                                // Detect database usage
                                if (content.contains("JpaRepository") || content.contains("@Entity")) {
                                    usage.getDatabaseUsers().add(module.getName());
                                }
                                if (content.contains("JdbcTemplate") || content.contains("NamedParameterJdbcTemplate")) {
                                    usage.getDatabaseUsers().add(module.getName());
                                }
                                if (content.contains("mybatis") || content.contains("MyBatis")) {
                                    usage.getDatabaseUsers().add(module.getName());
                                }

                                // Detect Redis usage
                                if (content.contains("RedisTemplate") || content.contains("@Cacheable")) {
                                    usage.getRedisUsers().add(module.getName());
                                }

                                // Detect Kafka usage
                                if (content.contains("@KafkaListener") || content.contains("KafkaTemplate")) {
                                    usage.getKafkaUsers().add(module.getName());
                                }

                                // Detect MinIO/S3 usage
                                if (content.contains("MinioClient") || content.contains("AmazonS3")) {
                                    usage.getObjectStorageUsers().add(module.getName());
                                }

                                // Detect Elasticsearch usage
                                if (content.contains("ElasticsearchRestTemplate") || content.contains("RestHighLevelClient")) {
                                    usage.getElasticsearchUsers().add(module.getName());
                                }

                            } catch (IOException e) {
                                log.warn("Failed to read file: {}", javaFile, e);
                            }
                        });
            }
        }

        return usage;
    }

    /**
     * Analyze architectural layers
     */
    private List<LayerInfo> analyzeLayers(List<ModuleInfo> modules) throws IOException {
        Map<String, LayerInfo> layerMap = new LinkedHashMap<>();

        layerMap.put("api", new LayerInfo("API Layer", "REST controllers, WebSocket handlers"));
        layerMap.put("application", new LayerInfo("Application Layer", "Application services, use cases"));
        layerMap.put("domain", new LayerInfo("Domain Layer", "Business logic, entities, repositories"));
        layerMap.put("infrastructure", new LayerInfo("Infrastructure Layer", "External integrations, persistence"));

        for (ModuleInfo module : modules) {
            Path modulePath = Path.of(module.getPath());

            // Check for layer indicators in package structure
            if (hasPackage(modulePath, "controller") || hasPackage(modulePath, "rest") || hasPackage(modulePath, "api")) {
                layerMap.get("api").getModules().add(module.getName());
            }
            if (hasPackage(modulePath, "service") && !hasPackage(modulePath, "infrastructure")) {
                layerMap.get("application").getModules().add(module.getName());
            }
            if (hasPackage(modulePath, "domain") || hasPackage(modulePath, "model") || hasPackage(modulePath, "entity")) {
                layerMap.get("domain").getModules().add(module.getName());
            }
            if (hasPackage(modulePath, "infrastructure") || hasPackage(modulePath, "config") || hasPackage(modulePath, "adapter")) {
                layerMap.get("infrastructure").getModules().add(module.getName());
            }
        }

        return new ArrayList<>(layerMap.values());
    }

    /**
     * Generate architecture recommendations
     */
    private List<String> generateRecommendations(ArchitectureReport report) {
        List<String> recommendations = new ArrayList<>();

        // Check for circular dependencies
        if (!report.getCircularDependencies().isEmpty()) {
            recommendations.add("⚠️ **CRITICAL**: Found " + report.getCircularDependencies().size() +
                    " circular dependencies. Consider introducing abstraction layers or events to break cycles.");
        }

        // Check layer separation
        long apiModules = report.getLayers().stream()
                .filter(l -> l.getName().contains("API"))
                .count();
        if (apiModules == 0) {
            recommendations.add("📋 Consider separating API layer from business logic for better maintainability.");
        }

        // Check database access patterns
        if (report.getInfrastructureUsage().getDatabaseUsers().size() == report.getModules().size()) {
            recommendations.add("💡 All modules access the database. Consider implementing CQRS or separate read/write models.");
        }

        // Check for shared kernel
        if (report.getModules().size() > 3) {
            recommendations.add("🏗️ With " + report.getModules().size() + " modules, consider defining a shared kernel for common domain concepts.");
        }

        // Check infrastructure abstraction
        List<ModuleInfo> tightlyCoupledModules = report.getModules().stream()
                .filter(m -> {
                    try {
                        return hasDirectInfrastructureAccess(Path.of(m.getPath()));
                    } catch (IOException e) {
                        return false;
                    }
                })
                .toList();

        if (!tightlyCoupledModules.isEmpty()) {
            recommendations.add("🔧 Modules with direct infrastructure access: " +
                    tightlyCoupledModules.stream().map(ModuleInfo::getName).collect(Collectors.joining(", ")) +
                    ". Consider using repository pattern or dependency inversion.");
        }

        return recommendations;
    }

    // Helper methods

    private List<String> extractMavenModules(String pomContent) {
        List<String> modules = new ArrayList<>();
        Pattern pattern = Pattern.compile("<module>(.*?)</module>");
        Matcher matcher = pattern.matcher(pomContent);
        while (matcher.find()) {
            modules.add(matcher.group(1).trim());
        }
        return modules;
    }

    private String extractTag(String content, String tagName) {
        Pattern pattern = Pattern.compile("<" + tagName + "(?:\\s+[^>]*)?>(.*?)</" + tagName + ">");
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private Set<String> extractDependencies(String pomContent) {
        Set<String> dependencies = new HashSet<>();
        Pattern pattern = Pattern.compile("<artifactId>(.*?)</artifactId>");
        Matcher matcher = pattern.matcher(pomContent);
        while (matcher.find()) {
            String artifactId = matcher.group(1).trim();
            // Filter out common plugins and test dependencies
            if (!artifactId.contains("maven-plugin") && !artifactId.contains("spring-boot-starter-test")) {
                dependencies.add(artifactId);
            }
        }
        return dependencies;
    }

    private String determineModuleType(Path modulePath) throws IOException {
        if (hasPackage(modulePath, "controller")) {
            return "API";
        }
        if (hasPackage(modulePath, "domain")) {
            return "DOMAIN";
        }
        if (hasPackage(modulePath, "infrastructure")) {
            return "INFRASTRUCTURE";
        }
        return "APPLICATION";
    }

    private boolean hasPackage(Path modulePath, String packageName) throws IOException {
        Path srcMainJava = modulePath.resolve("src/main/java");
        if (!Files.exists(srcMainJava)) {
            return false;
        }
        return Files.walk(srcMainJava)
                .anyMatch(p -> p.toString().contains(packageName.replace('.', File.separatorChar)));
    }

    private boolean hasDirectInfrastructureAccess(Path modulePath) throws IOException {
        Path srcMainJava = modulePath.resolve("src/main/java");
        if (!Files.exists(srcMainJava)) {
            return false;
        }
        return Files.walk(srcMainJava)
                .filter(p -> p.toString().endsWith(".java"))
                .anyMatch(p -> {
                    try {
                        String content = Files.readString(p);
                        return content.contains("Autowired") &&
                               (content.contains("RestTemplate") || content.contains("KafkaTemplate"));
                    } catch (IOException e) {
                        return false;
                    }
                });
    }

    private int countJavaFiles(Path modulePath) throws IOException {
        Path srcMainJava = modulePath.resolve("src/main/java");
        if (!Files.exists(srcMainJava)) {
            return 0;
        }
        return (int) Files.walk(srcMainJava)
                .filter(p -> p.toString().endsWith(".java"))
                .count();
    }

    // DTOs

    @Data
    public static class ArchitectureReport {
        private String projectRoot;
        private Date analysisTime;
        private List<ModuleInfo> modules;
        private Map<String, Set<String>> dependencyGraph;
        private List<CircularDependency> circularDependencies;
        private Map<String, Set<String>> externalDependencies;
        private InfrastructureUsage infrastructureUsage;
        private List<LayerInfo> layers;
        private List<String> recommendations;
    }

    @Data
    public static class ModuleInfo {
        private String name;
        private String path;
        private String artifactId;
        private String groupId;
        private String version;
        private String description;
        private String type;
        private int javaFileCount;
    }

    @Data
    public static class CircularDependency {
        private List<String> cycle;
        private String description;
    }

    @Data
    public static class InfrastructureUsage {
        private Set<String> databaseUsers = new HashSet<>();
        private Set<String> redisUsers = new HashSet<>();
        private Set<String> kafkaUsers = new HashSet<>();
        private Set<String> objectStorageUsers = new HashSet<>();
        private Set<String> elasticsearchUsers = new HashSet<>();
    }

    @Data
    public static class LayerInfo {
        private String name;
        private String description;
        private Set<String> modules = new HashSet<>();

        public LayerInfo(String name, String description) {
            this.name = name;
            this.description = description;
        }
    }
}
