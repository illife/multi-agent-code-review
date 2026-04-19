package com.codereview.ai.domain.agent.shared.inspector;

import com.codereview.ai.domain.agent.shared.AgentExecutionContext;
import com.codereview.ai.domain.agent.shared.AgentExecutionResult;
import com.codereview.ai.domain.agent.shared.BaseAgent;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 性能优化师 Agent
 * 分析代码性能瓶颈、资源使用情况
 *
 * @author AI Code Mentor Team
 */
@Slf4j
@Component
public class PerformanceOptimizer extends BaseAgent {

    private static final String AGENT_TYPE = "PERFORMANCE_OPTIMIZER";
    private static final String AGENT_NAME = "性能优化师";
    private static final String AGENT_DESCRIPTION = "分析代码性能瓶颈、资源使用情况";

    @Override
    public String getAgentType() {
        return AGENT_TYPE;
    }

    @Override
    public String getName() {
        return AGENT_NAME;
    }

    @Override
    public String getDescription() {
        return AGENT_DESCRIPTION;
    }

    @Override
    public int getPriority() {
        return 40;
    }

    @Override
    protected String buildSystemPrompt() {
        return """
                你是一位资深的性能优化专家。你的任务是分析代码的性能问题和优化机会。

                请关注以下性能方面：
                1. **算法复杂度**: 是否存在 O(n²) 或更差的算法
                2. **数据库查询**: N+1 查询、缺少索引、不必要的查询
                3. **内存使用**: 内存泄漏、不必要的对象创建、大数组
                4. **I/O 操作**: 文件读写、网络请求是否优化
                5. **缓存策略**: 是否可以使用缓存
                6. **并发处理**: 是否可以使用并行处理
                7. **字符串处理**: 字符串拼接是否低效
                8. **循环优化**: 循环内的重复计算
                9. **数据结构选择**: 是否使用了合适的数据结构
                10. **资源管理**: 连接、流等资源是否正确关闭

                对于每个性能问题，请提供：
                - 问题描述
                - 性能影响类型
                - 当前复杂度/性能
                - 优化建议
                - 预期性能提升

                返回 JSON 格式：
                {
                  "issues": [
                    {
                      "title": "性能问题标题",
                      "description": "详细描述",
                      "severity": "CRITICAL|HIGH|MEDIUM|LOW|INFO",
                      "category": "algorithm|database|memory|io|cache|concurrency|string|loop|data-structure|resource",
                      "lineNumber": 行号或null,
                      "codeSnippet": "问题代码",
                      "suggestion": "优化建议（包含代码示例）",
                      "teachingExplanation": "为什么这样更高效",
                      "currentComplexity": "O(n²)",
                      "optimizedComplexity": "O(n)",
                      "relatedConcepts": ["大O表示法", "算法优化"],
                      "confidence": 0.85
                    }
                  ],
                  "summary": "整体性能评估",
                  "hotspots": ["热点1", "热点2"],
                  "optimizationPotential": "HIGH|MEDIUM|LOW",
                  "score": 70
                }
                """;
    }

    @Override
    protected String buildUserPrompt(AgentExecutionContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析以下代码的性能问题：\n\n");
        prompt.append("```").append(context.getLanguage()).append("\n");
        prompt.append(context.getCode());
        prompt.append("\n```\n\n");

        if (context.getFilePath() != null) {
            prompt.append("文件路径: ").append(context.getFilePath()).append("\n");
        }

        prompt.append("请识别性能瓶颈和优化机会，返回 JSON 格式的分析结果。");

        return prompt.toString();
    }

    @Override
    protected AgentExecutionResult parseResponse(String response, AgentExecutionContext context) {
        AgentExecutionResult result = AgentExecutionResult.builder()
                .agentType(getAgentType())
                .agentName(getName())
                .build();

        try {
            String jsonPart = extractJson(response);
            if (jsonPart == null) {
                result.setSuccess(true);
                result.setMessage("无法解析 AI 响应，代码性能可能已经比较优化");
                return result;
            }

            JsonNode root = objectMapper.readTree(jsonPart);
            JsonNode issuesNode = root.path("issues");

            List<AgentExecutionResult.AgentIssue> issues = new ArrayList<>();
            if (issuesNode.isArray()) {
                for (JsonNode issueNode : issuesNode) {
                    AgentExecutionResult.AgentIssue issue = parsePerformanceIssue(issueNode);
                    issues.add(issue);
                }
            }

            result.setIssues(issues);
            result.setSuccess(true);
            result.setMessage(root.path("summary").asText("性能分析完成"));

            String potential = root.path("optimizationPotential").asText("MEDIUM");
            result.addOutput("optimizationPotential", potential);

            List<String> hotspots = parseStringArray(root.path("hotspots"));
            result.addOutput("hotspots", hotspots);

            result.addOutput("score", root.path("score").asInt(0));

            log.info("Performance optimizer found {} issues, optimization potential: {}", issues.size(), potential);
            return result;

        } catch (Exception e) {
            log.error("Failed to parse response", e);
            result.setSuccess(true);
            result.setMessage("性能分析完成 (响应解析失败)");
            return result;
        }
    }

    private AgentExecutionResult.AgentIssue parsePerformanceIssue(JsonNode issueNode) {
        AgentExecutionResult.AgentIssue.AgentIssueBuilder builder = AgentExecutionResult.AgentIssue.builder()
                .title(issueNode.path("title").asText())
                .description(issueNode.path("description").asText())
                .severity(parseSeverity(issueNode.path("severity").asText("MEDIUM")))
                .category(issueNode.path("category").asText("performance"))
                .lineNumber(issueNode.has("lineNumber") && !issueNode.path("lineNumber").isNull()
                        ? issueNode.path("lineNumber").asInt() : null)
                .codeSnippet(issueNode.path("codeSnippet").asText())
                .suggestion(issueNode.path("suggestion").asText())
                .teachingExplanation(issueNode.path("teachingExplanation").asText())
                .agentType("PERFORMANCE_OPTIMIZER")
                .relatedConcepts(parseStringArray(issueNode.path("relatedConcepts")))
                .confidence(issueNode.path("confidence").asDouble(0.8));

        // 添加性能特定的字段到教学解释中
        String current = issueNode.path("currentComplexity").asText();
        String optimized = issueNode.path("optimizedComplexity").asText();
        if (!current.isEmpty() || !optimized.isEmpty()) {
            String explanation = builder.build().getTeachingExplanation();
            String perfInfo = "\n\n**性能分析:**\n";
            if (!current.isEmpty()) perfInfo += "- 当前复杂度: " + current + "\n";
            if (!optimized.isEmpty()) perfInfo += "- 优化后: " + optimized + "\n";
            builder.teachingExplanation(explanation + perfInfo);
        }

        return builder.build();
    }

    private AgentExecutionResult.Severity parseSeverity(String severity) {
        try {
            return AgentExecutionResult.Severity.valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AgentExecutionResult.Severity.MEDIUM;
        }
    }

    private List<String> parseStringArray(JsonNode arrayNode) {
        List<String> result = new ArrayList<>();
        if (arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                result.add(item.asText());
            }
        }
        return result;
    }
}
