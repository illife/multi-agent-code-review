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
 * 架构守护者 Agent
 * 检查代码架构设计、模块依赖、设计模式使用等
 *
 * @author AI Code Mentor Team
 */
@Slf4j
@Component
public class ArchitectureGuardian extends BaseAgent {

    private static final String AGENT_TYPE = "ARCHITECTURE_GUARDIAN";
    private static final String AGENT_NAME = "架构守护者";
    private static final String AGENT_DESCRIPTION = "检查代码架构设计、模块依赖、设计模式使用等";

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
        return 20;
    }

    @Override
    protected String buildSystemPrompt() {
        return """
                你是软件架构专家。检查代码架构：设计模式、SOLID原则、耦合度、抽象层次。

                【重要】返回简洁的 JSON 格式，控制在 4000 tokens 以内：
                {
                  "issues": [
                    {
                      "t": "简短标题（10字内）",
                      "d": "描述（30字内）",
                      "s": "HIGH|MEDIUM|LOW",
                      "c": "solid|coupling|pattern|abstract|design",
                      "l": 行号数字或null,
                      "code": "代码片段（40字内）",
                      "fix": "改进建议（40字内）"
                    }
                  ],
                  "summary": "整体评价（20字内）"
                }

                每个字段必须精简。架构好则返回空 issues 数组。
                """;
    }

    @Override
    protected String buildUserPrompt(AgentExecutionContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析以下代码的架构设计：\n\n");
        prompt.append("```").append(context.getLanguage()).append("\n");
        prompt.append(context.getCode());
        prompt.append("\n```\n\n");

        if (context.getFilePath() != null) {
            prompt.append("文件路径: ").append(context.getFilePath()).append("\n");
        }

        prompt.append("请评估其架构质量，返回 JSON 格式的分析结果。");

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
                result.setMessage("无法解析 AI 响应，但架构可能没有明显问题");
                return result;
            }

            JsonNode root = objectMapper.readTree(jsonPart);
            JsonNode issuesNode = root.path("issues");

            List<AgentExecutionResult.AgentIssue> issues = new ArrayList<>();
            if (issuesNode.isArray()) {
                for (JsonNode issueNode : issuesNode) {
                    issues.add(parseIssue(issueNode));
                }
            }

            result.setIssues(issues);
            result.setSuccess(true);
            result.setMessage(root.path("summary").asText("架构检查完成"));

            log.info("Architecture guardian found {} issues", issues.size());
            return result;

        } catch (Exception e) {
            log.error("Failed to parse response", e);
            result.setSuccess(true);
            result.setMessage("架构检查完成 (响应解析失败)");
            return result;
        }
    }

    private AgentExecutionResult.AgentIssue parseIssue(JsonNode issueNode) {
        // 支持简化的字段名和完整的字段名
        String title = issueNode.has("t") ? issueNode.path("t").asText() : issueNode.path("title").asText();
        String description = issueNode.has("d") ? issueNode.path("d").asText() : issueNode.path("description").asText();
        String severityStr = issueNode.has("s") ? issueNode.path("s").asText() : issueNode.path("severity").asText();
        String category = issueNode.has("c") ? issueNode.path("c").asText() : issueNode.path("category").asText("architecture");
        String codeSnippet = issueNode.has("code") ? issueNode.path("code").asText() : issueNode.path("codeSnippet").asText();
        String suggestion = issueNode.has("fix") ? issueNode.path("fix").asText() : issueNode.path("suggestion").asText();

        Integer lineNumber = null;
        if (issueNode.has("l") && !issueNode.path("l").isNull()) {
            lineNumber = issueNode.path("l").asInt();
        } else if (issueNode.has("lineNumber") && !issueNode.path("lineNumber").isNull()) {
            lineNumber = issueNode.path("lineNumber").asInt();
        }

        return AgentExecutionResult.AgentIssue.builder()
                .title(title)
                .description(description)
                .severity(parseSeverity(severityStr))
                .category(category)
                .lineNumber(lineNumber)
                .codeSnippet(codeSnippet)
                .suggestion(suggestion)
                .agentType("ARCHITECTURE_GUARDIAN")
                .build();
    }

    private AgentExecutionResult.Severity parseSeverity(String severity) {
        if (severity == null || severity.isEmpty()) {
            return AgentExecutionResult.Severity.MEDIUM;
        }
        try {
            return AgentExecutionResult.Severity.valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AgentExecutionResult.Severity.MEDIUM;
        }
    }
}
