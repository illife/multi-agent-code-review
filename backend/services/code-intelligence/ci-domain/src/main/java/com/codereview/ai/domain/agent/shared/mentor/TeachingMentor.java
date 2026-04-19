package com.codereview.ai.domain.agent.shared.mentor;

import com.codereview.ai.domain.agent.shared.AgentExecutionContext;
import com.codereview.ai.domain.agent.shared.AgentExecutionResult;
import com.codereview.ai.domain.agent.shared.BaseAgent;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 教学导师 Agent
 * 根据用户水平提供个性化教学指导，生成代码审查教学报告
 *
 * @author AI Code Mentor Team
 */
@Slf4j
@Component
public class TeachingMentor extends BaseAgent {

    private static final String AGENT_TYPE = "TEACHING_MENTOR";
    private static final String AGENT_NAME = "教学导师";
    private static final String AGENT_DESCRIPTION = "根据用户水平提供个性化教学指导";

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
        return 100;
    }

    @Override
    protected String buildSystemPrompt() {
        return """
                你是一位经验丰富的编程导师。你的任务是根据用户的技能水平提供个性化的教学指导。

                教学原则：
                1. **适应性**: 根据用户水平调整教学内容深度
                2. **鼓励性**: 积极鼓励用户，建立学习信心
                3. **实践性**: 提供可操作的练习和示例
                4. **渐进性**: 从简单到复杂，循序渐进
                5. **关联性**: 将新知识与用户已有知识关联

                用户水平分类：
                - BEGINNER: 编程新手，需要详细解释和简单示例
                - INTERMEDIATE: 有一定基础，可以讨论原理和最佳实践
                - ADVANCED: 经验丰富，可以深入讨论底层原理和优化

                返回 JSON 格式：
                {
                  "teachingContent": {
                    "title": "教学主题",
                    "introduction": "引入话题",
                    "concepts": [
                      {
                        "name": "概念名称",
                        "explanation": "适合用户水平的解释",
                        "example": "代码示例",
                        "whyItMatters": "为什么重要",
                        "commonMistakes": ["常见错误1", "常见错误2"]
                      }
                    ],
                    "exercises": [
                      {
                        "title": "练习标题",
                        "description": "练习描述",
                        "difficulty": "EASY|MEDIUM|HARD",
                        "hints": ["提示1", "提示2"],
                        "expectedOutcome": "预期结果"
                      }
                    ],
                    "furtherLearning": [
                      {
                        "topic": "相关主题",
                        "reason": "学习这个主题的原因"
                      }
                    ],
                    "encouragement": "鼓励的话语"
                  },
                  "userLevel": "评估的用户水平",
                  "nextSteps": ["下一步建议1", "下一步建议2"]
                }
                """;
    }

    /**
     * 专门用于生成代码审查教学报告的系统提示词
     */
    public String buildTeachingReportSystemPrompt() {
        return """
                你是一位经验丰富的编程导师，负责为代码审查结果生成教学报告。

                你的任务是：
                1. 分析代码中发现的问题，识别用户的薄弱知识点
                2. 提供针对性的教学指导和改进建议
                3. 推荐学习资源和练习
                4. 给予鼓励，保持学习积极性

                返回 JSON 格式：
                {
                  "teachingReport": {
                    "summary": "整体总结（2-3句话）",
                    "codeSummary": "代码功能概述",
                    "knowledgeGaps": ["知识点缺口1", "知识点缺口2"],
                    "keyFindings": [
                      {
                        "category": "问题类别",
                        "issue": "问题描述",
                        "explanation": "为什么这是问题",
                        "improvement": "如何改进",
                        "severity": "CRITICAL|HIGH|MEDIUM|LOW"
                      }
                    ],
                    "learningResources": {
                      "主题1": ["资源1", "资源2"],
                      "主题2": ["资源1", "资源2"]
                    },
                    "priorityActions": ["优先行动1", "优先行动2"],
                    "encouragement": "鼓励的话语"
                  }
                }
                """;
    }

    /**
     * 构建教学报告用户提示词
     */
    public String buildTeachingReportUserPrompt(
            String codeContent,
            String language,
            String fileName,
            int totalIssues,
            int criticalIssues,
            int highIssues,
            int mediumIssues,
            int lowIssues
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为以下代码审查结果生成教学报告：\n\n");

        prompt.append("## 代码信息\n");
        prompt.append("文件名: ").append(fileName != null ? fileName : "未知").append("\n");
        prompt.append("编程语言: ").append(language != null ? language : "未知").append("\n\n");

        prompt.append("## 问题统计\n");
        prompt.append("总问题数: ").append(totalIssues).append("\n");
        prompt.append("- 严重问题 (CRITICAL): ").append(criticalIssues).append("\n");
        prompt.append("- 高危问题 (HIGH): ").append(highIssues).append("\n");
        prompt.append("- 中危问题 (MEDIUM): ").append(mediumIssues).append("\n");
        prompt.append("- 低危问题 (LOW): ").append(lowIssues).append("\n\n");

        prompt.append("## 代码内容\n");
        prompt.append("```").append(language != null ? language : "text").append("\n");
        // 限制代码长度，避免超出token限制
        String code = codeContent != null ? codeContent : "";
        if (code.length() > 3000) {
            code = code.substring(0, 3000) + "\n... (代码过长，已截断)";
        }
        prompt.append(code);
        prompt.append("\n```\n\n");

        prompt.append("请根据上述信息生成一个结构化的教学报告，帮助用户：\n");
        prompt.append("1. 理解代码中存在的问题\n");
        prompt.append("2. 学习相关的编程概念\n");
        prompt.append("3. 获得具体的改进建议\n");
        prompt.append("4. 推荐进一步学习的资源\n");
        prompt.append("5. 保持积极的学习态度\n");

        return prompt.toString();
    }

    @Override
    protected String buildUserPrompt(AgentExecutionContext context) {
        // 检查是否是教学报告生成模式
        if (context.getContextData("mode", "").equals("teaching_report")) {
            return buildTeachingReportUserPrompt(
                    context.getCode(),
                    context.getLanguage(),
                    context.getFilePath(),
                    context.getContextData("totalIssues", 0),
                    context.getContextData("criticalIssues", 0),
                    context.getContextData("highIssues", 0),
                    context.getContextData("mediumIssues", 0),
                    context.getContextData("lowIssues", 0)
            );
        }

        // 默认的通用教学模式
        String topic = context.getContextData("topic", "编程基础");
        String userLevel = context.getContextData("userLevel", "INTERMEDIATE");

        StringBuilder prompt = new StringBuilder();
        prompt.append("用户水平: ").append(userLevel).append("\n");
        prompt.append("教学主题: ").append(topic).append("\n");

        if (context.getCode() != null && !context.getCode().isEmpty()) {
            prompt.append("\n用户提供的代码:\n");
            prompt.append("```").append(context.getLanguage()).append("\n");
            prompt.append(context.getCode());
            prompt.append("\n```\n");
            prompt.append("\n请基于这段代码提供教学指导，帮助用户理解和改进。");
        } else {
            prompt.append("\n请提供关于该主题的完整教学内容。");
        }

        return prompt.toString();
    }

    @Override
    protected String buildSystemPromptForContext(AgentExecutionContext context) {
        // 检查是否是教学报告生成模式
        if (context.getContextData("mode", "").equals("teaching_report")) {
            return buildTeachingReportSystemPrompt();
        }
        return buildSystemPrompt();
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
                result.setSuccess(false);
                result.setMessage("无法解析 AI 响应");
                return result;
            }

            JsonNode root = objectMapper.readTree(jsonPart);

            // 检查是否是教学报告格式
            if (root.has("teachingReport")) {
                return parseTeachingReportResponse(root, result);
            }

            // 默认的教学内容格式
            return parseTeachingContentResponse(root, result);

        } catch (Exception e) {
            log.error("Failed to parse response", e);
            result.setSuccess(false);
            result.setMessage("教学内容生成失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 解析教学报告响应
     */
    private AgentExecutionResult parseTeachingReportResponse(JsonNode root, AgentExecutionResult result) {
        try {
            JsonNode report = root.path("teachingReport");

            result.setSuccess(true);
            result.setMessage("教学报告生成成功");

            // 提取教学报告的各个字段
            result.addOutput("summary", report.path("summary").asText());
            result.addOutput("codeSummary", report.path("codeSummary").asText());

            // 解析知识点缺口
            List<String> knowledgeGaps = parseStringArray(report.path("knowledgeGaps"));
            result.addOutput("knowledgeGaps", knowledgeGaps);

            // 解析关键发现
            JsonNode keyFindings = report.path("keyFindings");
            if (keyFindings.isArray()) {
                result.addOutput("keyFindings", keyFindings.toString());
            }

            // 解析学习资源
            JsonNode learningResources = report.path("learningResources");
            if (learningResources.isObject()) {
                result.addOutput("learningResources", learningResources.toString());
            }

            // 解析优先行动
            List<String> priorityActions = parseStringArray(report.path("priorityActions"));
            result.addOutput("priorityActions", priorityActions);

            // 鼓励话语
            result.addOutput("encouragement", report.path("encouragement").asText());

            log.info("Teaching report generated successfully with {} knowledge gaps", knowledgeGaps.size());
            return result;

        } catch (Exception e) {
            log.error("Failed to parse teaching report response", e);
            result.setSuccess(false);
            result.setMessage("教学报告解析失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 解析教学内容响应
     */
    private AgentExecutionResult parseTeachingContentResponse(JsonNode root, AgentExecutionResult result) {
        try {
            JsonNode teachingContent = root.path("teachingContent");

            result.setSuccess(true);
            result.setMessage(teachingContent.path("title").asText("教学内容"));
            result.addOutput("teachingContent", teachingContent.toString());
            result.addOutput("userLevel", root.path("userLevel").asText());

            List<String> nextSteps = parseStringArray(root.path("nextSteps"));
            result.addOutput("nextSteps", nextSteps);

            // 将教学内容转换为可读的格式
            String formattedContent = formatTeachingContent(teachingContent);
            result.addOutput("formattedContent", formattedContent);

            log.info("Teaching mentor generated content for user level: {}",
                    root.path("userLevel").asText());
            return result;

        } catch (Exception e) {
            log.error("Failed to parse teaching content response", e);
            result.setSuccess(false);
            result.setMessage("教学内容解析失败: " + e.getMessage());
            return result;
        }
    }

    private String formatTeachingContent(JsonNode content) {
        StringBuilder sb = new StringBuilder();

        sb.append("# ").append(content.path("title").asText()).append("\n\n");
        sb.append(content.path("introduction").asText()).append("\n\n");

        JsonNode concepts = content.path("concepts");
        if (concepts.isArray()) {
            sb.append("## 核心概念\n\n");
            for (JsonNode concept : concepts) {
                sb.append("### ").append(concept.path("name").asText()).append("\n");
                sb.append(concept.path("explanation").asText()).append("\n\n");

                if (concept.has("example")) {
                    sb.append("**示例:**\n```");
                    sb.append(concept.path("example").asText());
                    sb.append("```\n\n");
                }

                sb.append("**重要性**: ").append(concept.path("whyItMatters").asText()).append("\n\n");

                JsonNode mistakes = concept.path("commonMistakes");
                if (mistakes.isArray() && mistakes.size() > 0) {
                    sb.append("**常见错误:**\n");
                    for (JsonNode mistake : mistakes) {
                        sb.append("- ").append(mistake.asText()).append("\n");
                    }
                    sb.append("\n");
                }
            }
        }

        JsonNode exercises = content.path("exercises");
        if (exercises.isArray() && exercises.size() > 0) {
            sb.append("## 练习\n\n");
            for (JsonNode exercise : exercises) {
                sb.append("### ").append(exercise.path("title").asText()).append("\n");
                sb.append(exercise.path("description").asText()).append("\n");
                sb.append("**难度**: ").append(exercise.path("difficulty").asText()).append("\n\n");
            }
        }

        if (content.has("encouragement")) {
            sb.append("## 💡 ").append(content.path("encouragement").asText()).append("\n");
        }

        return sb.toString();
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
