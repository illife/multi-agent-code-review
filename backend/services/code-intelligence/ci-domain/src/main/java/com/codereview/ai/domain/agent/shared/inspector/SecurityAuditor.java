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
 * 安全审计员 Agent
 * 检查安全漏洞、SQL注入、XSS、敏感信息泄露等
 *
 * @author AI Code Mentor Team
 */
@Slf4j
@Component
public class SecurityAuditor extends BaseAgent {

    private static final String AGENT_TYPE = "SECURITY_AUDITOR";
    private static final String AGENT_NAME = "安全审计员";
    private static final String AGENT_DESCRIPTION = "检查安全漏洞、SQL注入、XSS、敏感信息泄露等";

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
        return 30;
    }

    @Override
    protected String buildSystemPrompt() {
        return """
                你是一位资深的安全专家。检查代码中的安全漏洞。

                重点检查：SQL注入、XSS、CSRF、敏感信息泄露、弱加密、路径遍历、权限缺失。

                【重要】返回简洁的 JSON 格式，控制在 5000 tokens 以内：
                {
                  "issues": [
                    {
                      "t": "简短标题（10字内）",
                      "d": "问题描述（30字内）",
                      "s": "CRITICAL|HIGH|MEDIUM|LOW",
                      "c": "sql-injection|xss|csrf|auth|crypto|path",
                      "l": 行号数字,
                      "code": "问题代码片段（50字内）",
                      "fix": "修复建议（50字内）"
                    }
                  ],
                  "summary": "整体评估（20字内）",
                  "riskLevel": "HIGH|MEDIUM|LOW"
                }

                注意：每个字段必须精简，优先报告高风险问题。
                """;
    }

    @Override
    protected String buildUserPrompt(AgentExecutionContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请检查以下代码的安全问题：\n\n");
        prompt.append("```").append(context.getLanguage()).append("\n");
        prompt.append(context.getCode());
        prompt.append("\n```\n\n");

        if (context.getFilePath() != null) {
            prompt.append("文件路径: ").append(context.getFilePath()).append("\n");
        }

        prompt.append("请识别所有安全风险，返回 JSON 格式的检查结果。");

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
                result.setMessage("无法解析 AI 响应，建议人工审查");
                result.addOutput("riskLevel", "UNKNOWN");
                return result;
            }

            JsonNode root = objectMapper.readTree(jsonPart);
            JsonNode issuesNode = root.path("issues");

            List<AgentExecutionResult.AgentIssue> issues = new ArrayList<>();
            if (issuesNode.isArray()) {
                for (JsonNode issueNode : issuesNode) {
                    AgentExecutionResult.AgentIssue issue = parseSecurityIssue(issueNode);
                    issues.add(issue);
                }
            }

            result.setIssues(issues);
            result.setSuccess(true);
            result.setMessage(root.path("summary").asText("安全检查完成"));

            String riskLevel = root.path("riskLevel").asText("MEDIUM");
            result.addOutput("riskLevel", riskLevel);

            log.info("Security auditor found {} issues, risk level: {}", issues.size(), riskLevel);
            return result;

        } catch (Exception e) {
            log.error("Failed to parse response", e);
            result.setSuccess(true);
            result.setMessage("安全检查完成 (响应解析失败)");
            result.addOutput("riskLevel", "UNKNOWN");
            return result;
        }
    }

    private AgentExecutionResult.AgentIssue parseSecurityIssue(JsonNode issueNode) {
        // 支持简化的字段名 (t, d, s, c, l, code, fix) 和完整的字段名
        String title = issueNode.has("t") ? issueNode.path("t").asText() : issueNode.path("title").asText();
        String description = issueNode.has("d") ? issueNode.path("d").asText() : issueNode.path("description").asText();
        String severityStr = issueNode.has("s") ? issueNode.path("s").asText() : issueNode.path("severity").asText();
        String category = issueNode.has("c") ? issueNode.path("c").asText() : issueNode.path("category").asText("security");
        String codeSnippet = issueNode.has("code") ? issueNode.path("code").asText() : issueNode.path("codeSnippet").asText();
        String suggestion = issueNode.has("fix") ? issueNode.path("fix").asText() : issueNode.path("suggestion").asText();

        // 行号可能是 l 或 lineNumber
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
                .agentType("SECURITY_AUDITOR")
                .build();
    }

    private AgentExecutionResult.Severity parseSeverity(String severity) {
        if (severity == null || severity.isEmpty()) {
            return AgentExecutionResult.Severity.HIGH;
        }
        try {
            return AgentExecutionResult.Severity.valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AgentExecutionResult.Severity.HIGH;
        }
    }
}
