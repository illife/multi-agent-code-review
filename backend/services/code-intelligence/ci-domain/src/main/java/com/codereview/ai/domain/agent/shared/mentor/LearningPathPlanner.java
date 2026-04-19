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
 * 学习路径规划师 Agent
 * 为用户定制学习路径
 *
 * @author AI Code Mentor Team
 */
@Slf4j
@Component
public class LearningPathPlanner extends BaseAgent {

    private static final String AGENT_TYPE = "LEARNING_PATH_PLANNER";
    private static final String AGENT_NAME = "学习路径规划师";
    private static final String AGENT_DESCRIPTION = "为用户定制学习路径";

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
        return 103;
    }

    @Override
    protected String buildSystemPrompt() {
        return """
                你是一位学习路径规划专家。你的任务是根据用户的目标和现有水平，制定个性化的学习路径。

                学习路径设计原则：
                1. **目标导向**: 紧密围绕用户的职业目标
                2. **循序渐进**: 从基础到高级，逐步深入
                3. **理论与实践结合**: 理论学习与实际项目并重
                4. **技能平衡**: 技术深度与广度的平衡
                5. **时间可行**: 考虑用户的时间投入能力
                6. **就业导向**: 关注市场需求和就业前景

                学习阶段：
                - foundation: 基础知识
                - intermediate: 进阶技能
                - advanced: 高级主题
                - practical: 实战项目
                - specialization: 专业领域

                资源类型：
                - course: 课程
                - book: 书籍
                - tutorial: 教程
                - project: 项目
                - practice: 练习
                - certification: 认证

                返回 JSON 格式：
                {
                  "learningPath": {
                    "title": "学习路径标题",
                    "description": "整体描述",
                    "targetRole": "目标角色",
                    "estimatedDuration": "预计总时长(周)",
                    "weeklyTimeCommitment": "建议每周投入时间(小时)",
                    "prerequisites": ["前置要求1", "前置要求2"],
                    "learningGoals": ["学习目标1", "学习目标2"],
                    "modules": [
                      {
                        "id": "module_id",
                        "order": 1,
                        "title": "模块标题",
                        "description": "模块描述",
                        "stage": "foundation|intermediate|advanced|practical|specialization",
                        "duration": "预计时长(周)",
                        "objectives": ["目标1", "目标2"],
                        "topics": [
                          {
                            "name": "主题名称",
                            "description": "主题描述",
                            "resources": [
                              {
                                "type": "course|book|tutorial|project|practice|certification",
                                "title": "资源标题",
                                "url": "链接(可选)",
                                "provider": "提供方",
                                "duration": "时长",
                                "difficulty": "EASY|MEDIUM|HARD",
                                "free": true
                              }
                            ],
                            "exercises": ["练习1", "练习2"],
                            "projects": ["项目1"]
                          }
                        ],
                        "deliverables": ["交付物1", "交付物2"],
                        "assessment": "评估方式"
                      }
                    ],
                    "milestones": [
                      {
                        "title": "里程碑",
                        "description": "描述",
                        "criteria": ["完成标准1", "完成标准2"]
                      }
                    ],
                    "recommendedProjects": [
                      {
                        "title": "项目名称",
                        "description": "项目描述",
                        "skills": ["技能1", "技能2"],
                        "difficulty": "MEDIUM",
                        "estimatedTime": "预计时长"
                      }
                    ],
                    "certifications": [
                      {
                        "name": "认证名称",
                        "provider": "提供方",
                        "priority": "HIGH|MEDIUM|LOW",
                        "url": "链接"
                      }
                    ],
                    "careerOutlook": {
                      "jobTitles": ["职位1", "职位2"],
                      "salaryRange": "薪资范围",
                      "demand": "市场需求",
                      "growthPath": "成长路径"
                    }
                  }
                }
                """;
    }

    @Override
    protected String buildUserPrompt(AgentExecutionContext context) {
        String targetSkill = context.getContextData("targetSkill", "全栈开发");
        String currentLevel = context.getContextData("currentLevel", "BEGINNER");
        String description = context.getContextData("description", "");
        String language = context.getLanguage();

        StringBuilder prompt = new StringBuilder();
        prompt.append("请为我定制一个学习路径：\n\n");
        prompt.append("目标技能: ").append(targetSkill).append("\n");
        prompt.append("当前水平: ").append(currentLevel).append("\n");
        prompt.append("编程语言: ").append(language).append("\n");

        if (description != null && !description.isEmpty()) {
            prompt.append("\n补充说明:\n").append(description).append("\n");
        }

        prompt.append("\n请制定一个详细的学习路径，返回 JSON 格式。");

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
            JsonNode learningPath = root.path("learningPath");

            result.setSuccess(true);
            result.setMessage("学习路径生成成功: " + learningPath.path("title").asText());
            result.addOutput("learningPath", learningPath.toString());

            // 提取关键信息
            result.addOutput("title", learningPath.path("title").asText());
            result.addOutput("description", learningPath.path("description").asText());
            result.addOutput("targetRole", learningPath.path("targetRole").asText());
            result.addOutput("estimatedDuration", learningPath.path("estimatedDuration").asInt());
            result.addOutput("weeklyTimeCommitment", learningPath.path("weeklyTimeCommitment").asInt());

            // 模块信息
            List<ModuleSummary> modules = parseModules(learningPath.path("modules"));
            result.addOutput("modules", modules);
            result.addOutput("moduleCount", modules.size());

            // 里程碑
            List<String> milestones = parseMilestones(learningPath.path("milestones"));
            result.addOutput("milestones", milestones);

            // 推荐项目
            List<String> projects = parseStringArray(learningPath.path("recommendedTitles"));
            result.addOutput("recommendedProjects", projects);

            log.info("Learning path generated: {}, {} modules, {} weeks",
                    learningPath.path("title").asText(),
                    modules.size(),
                    learningPath.path("estimatedDuration").asInt());

            return result;

        } catch (Exception e) {
            log.error("Failed to parse response", e);
            result.setSuccess(false);
            result.setMessage("学习路径生成失败: " + e.getMessage());
            return result;
        }
    }

    private List<ModuleSummary> parseModules(JsonNode modulesNode) {
        List<ModuleSummary> result = new ArrayList<>();
        if (modulesNode.isArray()) {
            for (JsonNode moduleNode : modulesNode) {
                result.add(ModuleSummary.builder()
                        .order(moduleNode.path("order").asInt())
                        .title(moduleNode.path("title").asText())
                        .stage(moduleNode.path("stage").asText())
                        .duration(moduleNode.path("duration").asInt())
                        .build());
            }
        }
        return result;
    }

    private List<String> parseMilestones(JsonNode milestonesNode) {
        List<String> result = new ArrayList<>();
        if (milestonesNode.isArray()) {
            for (JsonNode milestoneNode : milestonesNode) {
                result.add(milestoneNode.path("title").asText() + ": " +
                           milestoneNode.path("description").asText());
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
     * 模块摘要
     */
    @lombok.Data
    @lombok.Builder
    public static class ModuleSummary {
        private int order;
        private String title;
        private String stage;
        private int duration;
    }
}
