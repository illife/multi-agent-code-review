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
 * 技能评估师 Agent
 * 评估用户的编程技能水平
 *
 * @author AI Code Mentor Team
 */
@Slf4j
@Component
public class SkillAssessor extends BaseAgent {

    private static final String AGENT_TYPE = "SKILL_ASSESSOR";
    private static final String AGENT_NAME = "技能评估师";
    private static final String AGENT_DESCRIPTION = "评估用户的编程技能水平";

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
        return 101;
    }

    @Override
    protected String buildSystemPrompt() {
        return """
                你是一位技能评估专家。你的任务是评估用户的编程技能水平。

                评估维度：
                1. **代码质量**: 可读性、可维护性、规范性
                2. **算法理解**: 数据结构和算法的使用
                3. **架构设计**: 模块划分、设计模式应用
                4. **安全意识**: 安全编码实践
                5. **性能考虑**: 优化意识和实现
                6. **错误处理**: 异常处理和边界条件
                7. **测试意识**: 可测试性和测试覆盖

                技能水平定义：
                - BEGINNER (0-30): 基础语法理解，简单问题解决
                - INTERMEDIATE (31-60): 独立开发，理解基本概念
                - ADVANCED (61-80): 复杂问题解决，架构设计能力
                - EXPERT (81-100): 深入理解，最佳实践，性能优化

                返回 JSON 格式：
                {
                  "overallScore": 65,
                  "overallLevel": "INTERMEDIATE",
                  "dimensionScores": {
                    "codeQuality": {"score": 70, "level": "INTERMEDIATE", "strengths": [], "improvements": []},
                    "algorithms": {"score": 55, "level": "INTERMEDIATE", "strengths": [], "improvements": []},
                    "architecture": {"score": 60, "level": "INTERMEDIATE", "strengths": [], "improvements": []},
                    "security": {"score": 45, "level": "BEGINNER", "strengths": [], "improvements": []},
                    "performance": {"score": 50, "level": "INTERMEDIATE", "strengths": [], "improvements": []},
                    "errorHandling": {"score": 65, "level": "INTERMEDIATE", "strengths": [], "improvements": []},
                    "testing": {"score": 40, "level": "BEGINNER", "strengths": [], "improvements": []}
                  },
                  "strengths": ["优势1", "优势2"],
                  "improvements": ["改进建议1", "改进建议2"],
                  "recommendedTopics": [
                    {"topic": "推荐学习主题", "priority": "HIGH|MEDIUM|LOW", "reason": "原因"}
                  ],
                  "summary": "总体评价"
                }
                """;
    }

    @Override
    protected String buildUserPrompt(AgentExecutionContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请评估以下代码的编程技能水平：\n\n");
        prompt.append("编程语言: ").append(context.getLanguage()).append("\n\n");
        prompt.append("```").append(context.getLanguage()).append("\n");
        prompt.append(context.getCode());
        prompt.append("\n```\n\n");

        if (context.getFilePath() != null) {
            prompt.append("文件路径: ").append(context.getFilePath()).append("\n");
        }

        prompt.append("请从多个维度评估技能水平，返回 JSON 格式的评估结果。");

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
                result.setSuccess(false);
                result.setMessage("无法解析 AI 响应");
                return result;
            }

            JsonNode root = objectMapper.readTree(jsonPart);

            int overallScore = root.path("overallScore").asInt(50);
            String overallLevel = root.path("overallLevel").asText("INTERMEDIATE");

            result.setSuccess(true);
            result.setMessage("技能评估完成");
            result.addOutput("overallScore", overallScore);
            result.addOutput("overallLevel", overallLevel);
            result.addOutput("dimensionScores", root.path("dimensionScores").toString());

            List<String> strengths = parseStringArray(root.path("strengths"));
            result.addOutput("strengths", strengths);

            List<String> improvements = parseStringArray(root.path("improvements"));
            result.addOutput("improvements", improvements);

            List<SkillRecommendation> recommendations = parseRecommendations(root.path("recommendedTopics"));
            result.addOutput("recommendations", recommendations);

            result.addOutput("summary", root.path("summary").asText());

            log.info("Skill assessment completed: level={}, score={}", overallLevel, overallScore);
            return result;

        } catch (Exception e) {
            log.error("Failed to parse response", e);
            result.setSuccess(false);
            result.setMessage("技能评估失败: " + e.getMessage());
            return result;
        }
    }

    private List<SkillRecommendation> parseRecommendations(JsonNode arrayNode) {
        List<SkillRecommendation> result = new ArrayList<>();
        if (arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                result.add(SkillRecommendation.builder()
                        .topic(item.path("topic").asText())
                        .priority(item.path("priority").asText("MEDIUM"))
                        .reason(item.path("reason").asText())
                        .build());
            }
        }
        return result;
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

    /**
     * 技能推荐
     */
    @lombok.Data
    @lombok.Builder
    public static class SkillRecommendation {
        private String topic;
        private String priority;
        private String reason;
    }
}
