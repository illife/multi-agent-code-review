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
 * 练习教练 Agent
 * 生成和评估编程练习
 *
 * @author AI Code Mentor Team
 */
@Slf4j
@Component
public class ExerciseCoach extends BaseAgent {

    private static final String AGENT_TYPE = "EXERCISE_COACH";
    private static final String AGENT_NAME = "练习教练";
    private static final String AGENT_DESCRIPTION = "生成和评估编程练习";

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
        return 102;
    }

    @Override
    protected String buildSystemPrompt() {
        return """
                你是一位编程教练。你的任务是生成合适的编程练习或评估用户的练习提交。

                练习生成原则：
                1. **难度适中**: 根据用户水平设置合适的难度
                2. **目标明确**: 清晰的学习目标
                3. **循序渐进**: 从简单到复杂
                4. **实用性强**: 练习实际编程中常用的技能
                5. **趣味性**: 有趣的题目设计

                难度级别：
                - EASY: 基础练习，适合新手
                - MEDIUM: 中等练习，需要一定的思考
                - HARD: 挑战练习，考验综合能力

                练习类型：
                - algorithm: 算法练习
                - syntax: 语法练习
                - debugging: 调试练习
                - refactoring: 重构练习
                - design: 设计练习

                返回 JSON 格式（生成练习）：
                {
                  "exercise": {
                    "id": "exercise_id",
                    "title": "练习标题",
                    "description": "详细描述",
                    "difficulty": "EASY|MEDIUM|HARD",
                    "type": "algorithm|syntax|debugging|refactoring|design",
                    "estimatedTime": "预计耗时(分钟)",
                    "learningObjectives": ["目标1", "目标2"],
                    "requirements": ["要求1", "要求2"],
                    "starterCode": "起始代码",
                    "hints": ["提示1", "提示2", "提示3"],
                    "solution": "参考解决方案",
                    "testCases": [
                      {"input": "输入", "expectedOutput": "期望输出", "description": "描述"}
                    ],
                    "concepts": ["涉及的概念1", "概念2"],
                    "prerequisites": ["前置知识"],
                    "tags": ["标签1", "标签2"]
                  }
                }

                返回 JSON 格式（评估练习）：
                {
                  "evaluation": {
                    "score": 85,
                    "passedTests": 4,
                    "totalTests": 5,
                    "feedback": "整体反馈",
                    "strengths": ["做得好的地方1", "做得好的地方2"],
                    "improvements": ["改进建议1", "改进建议2"],
                    "detailedFeedback": [
                      {
                        "aspect": "代码质量",
                        "score": 80,
                        "comment": "详细评论"
                      }
                    ],
                    "nextExercise": "下一个推荐的练习",
                    "earnedBadges": ["徽章1"]
                  }
                }
                """;
    }

    @Override
    protected String buildUserPrompt(AgentExecutionContext context) {
        String mode = context.getContextData("mode", "generate");

        if ("evaluate".equals(mode)) {
            return buildEvaluatePrompt(context);
        } else {
            return buildGeneratePrompt(context);
        }
    }

    private String buildGeneratePrompt(AgentExecutionContext context) {
        String language = context.getLanguage();
        String difficulty = context.getContextData("difficulty", "MEDIUM");
        String type = context.getContextData("type", "algorithm");
        String topic = context.getContextData("topic", "");

        StringBuilder prompt = new StringBuilder();
        prompt.append("请生成一个编程练习：\n\n");
        prompt.append("编程语言: ").append(language).append("\n");
        prompt.append("难度级别: ").append(difficulty).append("\n");
        prompt.append("练习类型: ").append(type).append("\n");

        if (topic != null && !topic.isEmpty()) {
            prompt.append("主题/知识点: ").append(topic).append("\n");
        }

        prompt.append("\n请返回 JSON 格式的练习内容，包含起始代码、测试用例和参考解决方案。");

        return prompt.toString();
    }

    private String buildEvaluatePrompt(AgentExecutionContext context) {
        String language = context.getLanguage();
        String userCode = context.getCode();
        String exerciseDescription = context.getContextData("exerciseDescription", "");
        String expectedOutput = context.getContextData("expectedOutput", "");

        StringBuilder prompt = new StringBuilder();
        prompt.append("请评估用户的练习提交：\n\n");
        prompt.append("编程语言: ").append(language).append("\n");

        if (exerciseDescription != null && !exerciseDescription.isEmpty()) {
            prompt.append("练习要求: ").append(exerciseDescription).append("\n");
        }

        prompt.append("\n用户代码:\n```\n").append(language).append("\n");
        prompt.append(userCode);
        prompt.append("\n```\n");

        if (expectedOutput != null && !expectedOutput.isEmpty()) {
            prompt.append("\n期望输出: ").append(expectedOutput).append("\n");
        }

        prompt.append("\n请评估代码的正确性、质量和风格，返回 JSON 格式的评估结果。");

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

            // 检查是生成练习还是评估练习
            if (root.has("exercise")) {
                return parseExerciseResponse(root, result);
            } else if (root.has("evaluation")) {
                return parseEvaluationResponse(root, result);
            } else {
                result.setSuccess(false);
                result.setMessage("未知的响应格式");
                return result;
            }

        } catch (Exception e) {
            log.error("Failed to parse response", e);
            result.setSuccess(false);
            result.setMessage("响应解析失败: " + e.getMessage());
            return result;
        }
    }

    private AgentExecutionResult parseExerciseResponse(JsonNode root, AgentExecutionResult result) {
        JsonNode exercise = root.path("exercise");

        result.setSuccess(true);
        result.setMessage("练习生成成功: " + exercise.path("title").asText());
        result.addOutput("exercise", exercise.toString());

        // 提取关键信息
        result.addOutput("title", exercise.path("title").asText());
        result.addOutput("difficulty", exercise.path("difficulty").asText());
        result.addOutput("estimatedTime", exercise.path("estimatedTime").asInt());
        result.addOutput("starterCode", exercise.path("starterCode").asText());
        result.addOutput("hints", parseStringArray(exercise.path("hints")));

        log.info("Exercise generated: {}", exercise.path("title").asText());
        return result;
    }

    private AgentExecutionResult parseEvaluationResponse(JsonNode root, AgentExecutionResult result) {
        JsonNode evaluation = root.path("evaluation");

        result.setSuccess(true);
        result.setMessage("练习评估完成");

        int score = evaluation.path("score").asInt();
        int passedTests = evaluation.path("passedTests").asInt();
        int totalTests = evaluation.path("totalTests").asInt();

        result.addOutput("score", score);
        result.addOutput("passedTests", passedTests);
        result.addOutput("totalTests", totalTests);
        result.addOutput("feedback", evaluation.path("feedback").asText());
        result.addOutput("strengths", parseStringArray(evaluation.path("strengths")));
        result.addOutput("improvements", parseStringArray(evaluation.path("improvements")));
        result.addOutput("nextExercise", evaluation.path("nextExercise").asText());
        result.addOutput("earnedBadges", parseStringArray(evaluation.path("earnedBadges")));

        log.info("Exercise evaluated: score={}/100, tests={}/{}", score, passedTests, totalTests);
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
}
