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
 * SQL Injection Analyzer Tool
 *
 * Analyzes code for SQL injection vulnerabilities
 *
 * @author Code Review AI Team
 */
@Slf4j
@Component
public class SqlInjectionAnalyzerTool implements Tool {

    // SQL注入检测模式
    private static final List<SqlPattern> SQL_PATTERNS = List.of(
            new SqlPattern(
                    "java_string_concat",
                    Pattern.compile("String\\s+\\w+\\s*=\\s*\".*?\"\\s*\\+", Pattern.CASE_INSENSITIVE),
                    "Java字符串拼接SQL"
            ),
            new SqlPattern(
                    "java_execute_query",
                    Pattern.compile("executeQuery\\s*\\(.*?\\+.*?\\)", Pattern.CASE_INSENSITIVE),
                    "JDBC executeQuery拼接"
            ),
            new SqlPattern(
                    "java_statement",
                    Pattern.compile("Statement\\.executeUpdate?\\s*\\(.*?\\+.*?\\)", Pattern.CASE_INSENSITIVE),
                    "Statement直接拼接"
            ),
            new SqlPattern(
                    "javascript_template",
                    Pattern.compile("const\\s+sql\\s*=\\s*`.*?\\$\\{.*?\\}.*?`", Pattern.CASE_INSENSITIVE),
                    "JavaScript模板字符串"
            ),
            new SqlPattern(
                    "python_format",
                    Pattern.compile("query\\s*=\\s*[\"'].*?\\{.*?\\}.*?['\"]", Pattern.CASE_INSENSITIVE),
                    "Python f-string格式化"
            ),
            new SqlPattern(
                    "python_percent",
                    Pattern.compile("query\\s*=\\s*[\"'].*?%s.*?['\"]", Pattern.CASE_INSENSITIVE),
                    "Python % 格式化"
            ),
            new SqlPattern(
                    "php_direct",
                    Pattern.compile("\\$\\w+\\s*\\.", Pattern.CASE_INSENSITIVE),
                    "PHP直接变量插值"
            )
    );

    @Override
    public String getName() {
        return "sql_injection_analyzer";
    }

    @Override
    public String getDescription() {
        return "分析代码中的SQL注入漏洞，检测不安全的SQL拼接和参数化查询缺失。支持Java、JavaScript、Python、PHP等多语言";
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
                "language": {
                  "type": "string",
                  "description": "编程语言",
                  "enum": ["java", "javascript", "python", "php", "go", "csharp"]
                },
                "check_lines": {
                  "type": "boolean",
                  "description": "是否返回具体行号",
                  "default": true
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
            String language = context != null ? context.getLanguage() :
                    (String) parameters.getOrDefault("language", "java");

            List<Map<String, Object>> vulnerabilities = analyzeSqlInjection(code, language);

            return ToolExecutionResult.builder()
                    .success(true)
                    .data(Map.of(
                            "vulnerabilities", vulnerabilities,
                            "count", vulnerabilities.size(),
                            "language", language,
                            "summary", vulnerabilities.isEmpty() ? "未发现SQL注入漏洞" :
                                    String.format("发现%d个SQL注入风险点", vulnerabilities.size())
                    ))
                    .executionTimeMs(System.currentTimeMillis() - start)
                    .build();

        } catch (Exception e) {
            log.error("SQL injection analysis failed", e);
            return ToolExecutionResult.failure(e.getMessage());
        }
    }

    /**
     * 分析SQL注入漏洞
     */
    private List<Map<String, Object>> analyzeSqlInjection(String code, String language) {
        List<Map<String, Object>> vulnerabilities = new ArrayList<>();

        String[] lines = code.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // 跳过注释和空行
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("#") ||
                line.startsWith("/*") || line.startsWith("*")) {
                continue;
            }

            for (SqlPattern pattern : SQL_PATTERNS) {
                Matcher matcher = pattern.pattern().matcher(line);

                if (matcher.find()) {
                    Map<String, Object> vuln = new HashMap<>();
                    vuln.put("lineNumber", i + 1);
                    vuln.put("severity", "HIGH");
                    vuln.put("code", line);
                    vuln.put("patternName", pattern.name());
                    vuln.put("description", getDescriptionForPattern(pattern.name()));
                    vuln.put("recommendation", getRecommendationForPattern(pattern.name(), language));
                    vulnerabilities.add(vuln);
                    break; // 一行只报告一个问题
                }
            }
        }

        return vulnerabilities;
    }

    private String getDescriptionForPattern(String patternName) {
        return switch (patternName) {
            case "java_string_concat" -> "使用字符串拼接构建SQL语句，可能导致SQL注入";
            case "java_execute_query" -> "executeQuery方法中使用了字符串拼接";
            case "java_statement" -> "使用Statement而非PreparedStatement，存在注入风险";
            case "javascript_template" -> "模板字符串直接插入变量，未做转义";
            case "python_format" -> "使用f-string或format()直接拼接SQL";
            case "python_percent" -> "使用%格式化符直接拼接SQL";
            case "php_direct" -> "PHP变量直接插值到SQL字符串中";
            default -> "检测到潜在的SQL注入风险";
        };
    }

    private String getRecommendationForPattern(String patternName, String language) {
        return switch (patternName) {
            case "java_string_concat", "java_execute_query", "java_statement" ->
                "使用PreparedStatement替代Statement，采用参数化查询";
            case "javascript_template" ->
                "使用参数化查询库（如pg-promise）或对用户输入进行转义";
            case "python_format", "python_percent" ->
                "使用参数化查询（如psycopg2的参数绑定）或ORM框架";
            case "php_direct" ->
                "使用预处理语句（Prepared Statements）或PDO参数绑定";
            default -> "使用参数化查询，避免直接字符串拼接";
        };
    }

    @Override
    public boolean supportsLanguage(String language) {
        return Set.of("java", "javascript", "python", "go", "php", "csharp", "typescript")
                .contains(language.toLowerCase());
    }

    @Override
    public Tool.ToolCategory getCategory() {
        return Tool.ToolCategory.SECURITY_SCAN;
    }

    @Override
    public int getPriority() {
        return 10; // 高优先级
    }

    @Override
    public boolean isFree() {
        return true; // 这是免费工具
    }

    /**
     * SQL检测模式
     */
    private record SqlPattern(
            String name,
            Pattern pattern,
            String description
    ) {}
}
