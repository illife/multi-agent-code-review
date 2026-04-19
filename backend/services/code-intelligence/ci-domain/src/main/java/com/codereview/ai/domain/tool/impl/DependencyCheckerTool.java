package com.codereview.ai.domain.tool.impl;

import com.codereview.ai.domain.tool.Tool;
import com.codereview.ai.domain.tool.ToolContext;
import com.codereview.ai.domain.tool.ToolExecutionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.codereview.ai.domain.tool.Tool.ToolCategory;

import java.util.*;

/**
 * Dependency Checker Tool
 *
 * Checks project dependencies for known vulnerabilities
 *
 * @author Code Review AI Team
 */
@Slf4j
@Component
public class DependencyCheckerTool implements Tool {

    // 模拟的已知漏洞数据库
    private static final Map<String, VulnerabilityInfo> KNOWN_VULNERABILITIES = Map.of(
            "lodash", new VulnerabilityInfo("4.17.15", "CVE-2019-10744", "HIGH",
                    "Prototype pollution in lodash before 4.17.19"),
            "axios", new VulnerabilityInfo("0.19.0", "CVE-2023-45857", "MEDIUM",
                    "SSRF vulnerability in axios"),
            "express", new VulnerabilityInfo("4.17.3", "CVE-2022-02823", "HIGH",
                    "Path traversal in express"),
            "react", new VulnerabilityInfo("16.3.0", "CVE-2021-22926", "MEDIUM",
                    "XSS in react-dom"),
            "vue", new VulnerabilityInfo("2.6.0", "CVE-2021-22184", "LOW",
                    "Template injection in Vue"),
            "jquery", new VulnerabilityInfo("3.5.0", "CVE-2021-4117", "HIGH",
                    "XSS in jQuery")
    );

    @Override
    public String getName() {
        return "dependency_checker";
    }

    @Override
    public String getDescription() {
        return "检查项目依赖中的已知漏洞和安全问题。支持Java（Maven）、JavaScript/TypeScript（npm）、Python（pip）等主流包管理器";
    }

    @Override
    public String getParameterSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "language": {
                  "type": "string",
                  "description": "编程语言/包管理器类型",
                  "enum": ["java", "javascript", "typescript", "python", "go", "php"]
                },
                "dependencies": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "name": {"type": "string"},
                      "version": {"type": "string"}
                    },
                    "required": ["name"]
                  },
                  "description": "依赖列表，格式: [{\"name\": \"lodash\", \"version\": \"4.17.15\"}]"
                }
              },
              "required": ["language", "dependencies"]
            }
            """;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters, ToolContext context) {
        long start = System.currentTimeMillis();

        try {
            String language = (String) parameters.get("language");

            @SuppressWarnings("unchecked")
            List<Map<String, String>> dependencies =
                    (List<Map<String, String>>) parameters.get("dependencies");

            if (dependencies == null || dependencies.isEmpty()) {
                return ToolExecutionResult.success(Map.of(
                        "vulnerabilities", List.of(),
                        "count", 0,
                        "message", "未发现依赖漏洞"
                ));
            }

            List<Map<String, Object>> vulnerabilities = checkVulnerabilities(dependencies);

            return ToolExecutionResult.builder()
                    .success(true)
                    .data(Map.of(
                            "vulnerabilities", vulnerabilities,
                            "count", vulnerabilities.size(),
                            "language", language,
                            "summary", vulnerabilities.isEmpty() ? "未发现已知依赖漏洞" :
                                    String.format("发现%d个依赖存在已知漏洞", vulnerabilities.size())
                    ))
                    .executionTimeMs(System.currentTimeMillis() - start)
                    .build();

        } catch (Exception e) {
            log.error("Dependency checking failed", e);
            return ToolExecutionResult.failure(e.getMessage());
        }
    }

    /**
     * 检查依赖漏洞
     */
    private List<Map<String, Object>> checkVulnerabilities(List<Map<String, String>> dependencies) {
        List<Map<String, Object>> vulnerabilities = new ArrayList<>();

        for (Map<String, String> dep : dependencies) {
            String name = dep.get("name");
            String version = dep.get("version");

            VulnerabilityInfo vulnInfo = KNOWN_VULNERABILITIES.get(name);

            if (vulnInfo != null) {
                // 简单的版本比较（实际应该使用语义版本比较）
                boolean isVulnerable = isVersionVulnerable(version, vulnInfo.vulnerableVersion());

                if (isVulnerable) {
                    Map<String, Object> vuln = new HashMap<>();
                    vuln.put("dependency", name);
                    vuln.put("currentVersion", version);
                    vuln.put("vulnerableVersion", vulnInfo.vulnerableVersion());
                    vuln.put("cve", vulnInfo.cve());
                    vuln.put("severity", vulnInfo.severity());
                    vuln.put("description", vulnInfo.description());
                    vuln.put("recommendation",
                            String.format("升级到 %s 或更高版本", vulnInfo.fixedVersion()));
                    vulnerabilities.add(vuln);
                }
            }
        }

        return vulnerabilities;
    }

    /**
     * 简单的版本比较（实际应使用语义版本比较库）
     */
    private boolean isVersionVulnerable(String currentVersion, String vulnerableVersion) {
        try {
            // 移除可能的 'v' 前缀
            String current = currentVersion.replace("^v", "").replace("^V", "");
            String vulnerable = vulnerableVersion.replace("^v", "").replace("^V", "");

            // 简单比较：如果当前版本小于等于有漏洞的版本，则存在风险
            return compareVersions(current, vulnerable) <= 0;
        } catch (Exception e) {
            log.warn("Failed to compare versions: {} vs {}", currentVersion, vulnerableVersion, e);
            return false;
        }
    }

    /**
     * 简单的版本号比较
     * 返回:负数表示v1<v2, 0表示相等, 正数表示v1>v2
     */
    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        int maxLength = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i].replaceAll("[^0-9]", "0")) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i].replaceAll("[^0-9]", "0")) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }

        return 0;
    }

    @Override
    public boolean supportsLanguage(String language) {
        return true; // 支持所有语言
    }

    @Override
    public Tool.ToolCategory getCategory() {
        return Tool.ToolCategory.SECURITY_SCAN;
    }

    @Override
    public int getPriority() {
        return 30;
    }

    @Override
    public boolean isFree() {
        return true; // 本地数据库查询，免费
    }

    /**
     * 漏洞信息
     */
    private record VulnerabilityInfo(
            String vulnerableVersion,
            String cve,
            String severity,
            String description
    ) {
        String fixedVersion() {
            // 简单逻辑：将有漏洞版本的最后一位+1
            String[] parts = vulnerableVersion.split("\\.");
            int lastPart = Integer.parseInt(parts[parts.length - 1]);
            parts[parts.length - 1] = String.valueOf(lastPart + 1);
            return String.join(".", parts);
        }
    }
}
