package com.codereview.ai.domain.tool.impl;

import com.codereview.ai.domain.tool.Tool;
import com.codereview.ai.domain.tool.ToolContext;
import com.codereview.ai.domain.tool.ToolExecutionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.codereview.ai.domain.tool.Tool.ToolCategory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * XSS Vulnerability Scanner Tool
 *
 * Scans code for Cross-Site Scripting (XSS) vulnerabilities
 *
 * @author Code Review AI Team
 */
@Slf4j
@Component
public class XssScannerTool implements Tool {

    // XSS检测模式
    private static final List<XssPattern> XSS_PATTERNS = List.of(
            new XssPattern(
                    "innerHTML",
                    Pattern.compile("innerHTML\\s*\\+?=", Pattern.CASE_INSENSITIVE),
                    "直接设置innerHTML可能导致XSS",
                    "HIGH"
            ),
            new XssPattern(
                    "document.write",
                    Pattern.compile("document\\.write\\s*\\(", Pattern.CASE_INSENSITIVE),
                    "document.write可能执行恶意脚本",
                    "HIGH"
            ),
            new XssPattern(
                    "eval",
                    Pattern.compile("eval\\s*\\(", Pattern.CASE_INSENSITIVE),
                    "eval可以执行任意代码，极高风险",
                    "CRITICAL"
            ),
            new XssPattern(
                    "jquery_html",
                    Pattern.compile("\\$\\(.*?\\)\\.html\\s*\\(", Pattern.CASE_INSENSITIVE),
                    "jQuery.html()不转义HTML，存在XSS风险",
                    "HIGH"
            ),
            new XssPattern(
                    "outerHTML",
                    Pattern.compile("outerHTML\\s*\\+?=", Pattern.CASE_INSENSITIVE),
                    "outerHTML可能导致XSS",
                    "HIGH"
            ),
            new XssPattern(
                    "dangerouslySetInnerHTML",
                    Pattern.compile("dangerouslySetInnerHTML\\s*\\(", Pattern.CASE_INSENSITIVE),
                    "React的dangerouslySetInnerHTML明确告知有XSS风险",
                    "HIGH"
            ),
            new XssPattern(
                    "v_html",
                    Pattern.compile("v-html\\s*=\\s*[\"'].*?[\"']", Pattern.CASE_INSENSITIVE),
                    "Vue的v-html指令需要确保输入已转义",
                    "MEDIUM"
            )
    );

    @Override
    public String getName() {
        return "xss_scanner";
    }

    @Override
    public String getDescription() {
        return "扫描代码中的跨站脚本(XSS)漏洞，检测未经过滤的用户输入直接输出到页面。支持JavaScript、TypeScript、React、Vue等前端技术栈";
    }

    @Override
    public String getParameterSchema() {
        return """
            {
              "type": "object",
              "properties": {
                "code": {
                  "type": "string",
                  "description": "要分析的代码片段"
                },
                "framework": {
                  "type": "string",
                  "description": "前端框架类型",
                  "enum": ["react", "vue", "angular", "vanilla", "auto"],
                  "default": "auto"
                }
              },
              "required": ["code"]
            }
            """;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters, ToolContext context) {
        long start = System.currentTimeMillis();

        try {
            String code = (String) parameters.get("code");

            List<Map<String, Object>> xssPoints = scanXssVulnerabilities(code);

            return ToolExecutionResult.builder()
                    .success(true)
                    .data(Map.of(
                            "xssPoints", xssPoints,
                            "count", xssPoints.size(),
                            "summary", xssPoints.isEmpty() ? "未发现XSS漏洞" :
                                    String.format("发现%d个XSS风险点", xssPoints.size())
                    ))
                    .executionTimeMs(System.currentTimeMillis() - start)
                    .build();

        } catch (Exception e) {
            log.error("XSS scanning failed", e);
            return ToolExecutionResult.failure(e.getMessage());
        }
    }

    /**
     * 扫描XSS漏洞
     */
    private List<Map<String, Object>> scanXssVulnerabilities(String code) {
        List<Map<String, Object>> xssPoints = new ArrayList<>();

        String[] lines = code.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // 跳过注释和空行
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("/*") ||
                line.startsWith("*") || line.startsWith("<!--")) {
                continue;
            }

            for (XssPattern pattern : XSS_PATTERNS) {
                Matcher matcher = pattern.pattern().matcher(line);

                if (matcher.find()) {
                    Map<String, Object> xssPoint = new HashMap<>();
                    xssPoint.put("lineNumber", i + 1);
                    xssPoint.put("severity", pattern.severity());
                    xssPoint.put("code", line);
                    xssPoint.put("patternName", pattern.name());
                    xssPoint.put("description", pattern.description());
                    xssPoint.put("recommendation", getRecommendationForPattern(pattern.name()));
                    xssPoints.add(xssPoint);
                    break;
                }
            }
        }

        return xssPoints;
    }

    private String getRecommendationForPattern(String patternName) {
        return switch (patternName) {
            case "innerHTML" ->
                "使用textContent替代innerHTML，或对输入进行HTML转义";
            case "document.write" ->
                "避免使用document.write，使用DOM操作方法";
            case "eval" ->
                "避免使用eval，改用JSON.parse或其他安全方法";
            case "jquery_html" ->
                "使用.text()替代.html()，或对输入先进行转义";
            case "outerHTML" ->
                "谨慎使用outerHTML，确保输入已转义";
            case "dangerouslySetInnerHTML" ->
                "仅在确认输入安全时使用，或使用DOMPurify等库清理";
            case "v_html" ->
                "确保用户输入已转义，或仅显示可信内容";
            default ->
                "对所有用户输入进行转义后再输出到HTML";
        };
    }

    @Override
    public boolean supportsLanguage(String language) {
        return Set.of("javascript", "typescript", "java", "php", "vue", "react", "angular")
                .contains(language.toLowerCase());
    }

    @Override
    public Tool.ToolCategory getCategory() {
        return Tool.ToolCategory.SECURITY_SCAN;
    }

    @Override
    public int getPriority() {
        return 20;
    }

    @Override
    public boolean isFree() {
        return true;
    }

    /**
     * XSS检测模式
     */
    private record XssPattern(
            String name,
            Pattern pattern,
            String description,
            String severity
    ) {}
}
