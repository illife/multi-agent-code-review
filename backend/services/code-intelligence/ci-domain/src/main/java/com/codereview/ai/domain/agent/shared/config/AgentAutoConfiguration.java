package com.codereview.ai.domain.agent.shared.config;

import com.codereview.ai.domain.agent.shared.AgentAiService;
import com.codereview.ai.domain.agent.shared.AgentExecutionContext;
import com.codereview.ai.domain.agent.shared.AgentExecutionResult;
import com.codereview.ai.domain.agent.shared.AgentOrchestrationService;
import com.codereview.ai.domain.agent.shared.BaseAgent;
import com.codereview.ai.domain.agent.shared.inspector.ArchitectureGuardian;
import com.codereview.ai.domain.agent.shared.inspector.CodeStandardsInspector;
import com.codereview.ai.domain.agent.shared.inspector.PerformanceOptimizer;
import com.codereview.ai.domain.agent.shared.inspector.SecurityAuditor;
import com.codereview.ai.domain.agent.shared.mentor.ExerciseCoach;
import com.codereview.ai.domain.agent.shared.mentor.LearningPathPlanner;
import com.codereview.ai.domain.agent.shared.mentor.SkillAssessor;
import com.codereview.ai.domain.agent.shared.mentor.TeachingMentor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.think.platform.shared.ai.llm.ChatRequest;
import com.think.platform.shared.ai.llm.ChatResponse;
import com.think.platform.shared.ai.llm.LlmProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent 自动配置类
 * 自动配置和注册所有 Agent
 *
 * @author AI Code Mentor Team
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(AgentProperties.class)
@ConditionalOnProperty(prefix = "agent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AgentAutoConfiguration {

    @Autowired
    private LlmProvider llmProvider;

    @Autowired
    private CodeStandardsInspector codeStandardsInspector;

    @Autowired
    private ArchitectureGuardian architectureGuardian;

    @Autowired
    private SecurityAuditor securityAuditor;

    @Autowired
    private PerformanceOptimizer performanceOptimizer;

    @Autowired
    private TeachingMentor teachingMentor;

    @Autowired
    private SkillAssessor skillAssessor;

    @Autowired
    private ExerciseCoach exerciseCoach;

    @Autowired
    private LearningPathPlanner learningPathPlanner;

    /**
     * 创建 Agent AI 服务
     */
    @Bean
    public AgentAiService agentAiService() {
        return new AgentAiServiceImpl(llmProvider);
    }

    /**
     * 创建 Agent 注册表
     */
    @Bean
    public AgentRegistry agentRegistry() {
        AgentRegistry registry = new AgentRegistry();

        // 注册代码审查类 Agent
        registry.register("CODE_STANDARDS_INSPECTOR", codeStandardsInspector);
        registry.register("ARCHITECTURE_GUARDIAN", architectureGuardian);
        registry.register("SECURITY_AUDITOR", securityAuditor);
        registry.register("PERFORMANCE_OPTIMIZER", performanceOptimizer);

        // 注册教学类 Agent
        registry.register("TEACHING_MENTOR", teachingMentor);
        registry.register("SKILL_ASSESSOR", skillAssessor);
        registry.register("EXERCISE_COACH", exerciseCoach);
        registry.register("LEARNING_PATH_PLANNER", learningPathPlanner);

        log.info("Agent registry initialized with {} agents", registry.size());
        return registry;
    }

    /**
     * 创建 Agent 编排服务
     */
    @Bean
    @DependsOn("agentRegistry")
    public AgentOrchestrationService agentOrchestrationService(AgentRegistry agentRegistry, AgentAiService aiService) {
        return new AgentOrchestrationServiceImpl(agentRegistry, aiService);
    }

    /**
     * Agent AI 服务实现
     */
    private static class AgentAiServiceImpl implements AgentAiService {

        private final LlmProvider llmProvider;

        public AgentAiServiceImpl(LlmProvider llmProvider) {
            this.llmProvider = llmProvider;
        }

        @Override
        public String chat(String systemPrompt, String userPrompt) {
            ChatRequest request = ChatRequest.builder()
                    .systemPrompt(systemPrompt)
                    .build();
            request.addUserMessage(userPrompt);

            ChatResponse response = llmProvider.chat(request);

            if (!response.isSuccess()) {
                throw new RuntimeException("AI service call failed: " + response.getError());
            }

            return response.getContent();
        }

        @Override
        public String chatWithContext(String systemPrompt, String userPrompt, String conversationId) {
            // 对于简单实现，忽略对话上下文
            return chat(systemPrompt, userPrompt);
        }

        @Override
        public float[] embed(String text) {
            return llmProvider.embed(text);
        }
    }

    /**
     * Agent 注册表
     */
    public static class AgentRegistry {
        private final Map<String, BaseAgent> agents = new HashMap<>();

        public void register(String agentType, BaseAgent agent) {
            agents.put(agentType, agent);
            log.info("Registered agent: {} ({})", agent.getName(), agentType);
        }

        public BaseAgent getAgent(String agentType) {
            BaseAgent agent = agents.get(agentType);
            if (agent == null) {
                throw new IllegalArgumentException("Unknown agent type: " + agentType);
            }
            return agent;
        }

        public List<BaseAgent> getAllAgents() {
            return List.copyOf(agents.values());
        }

        public int size() {
            return agents.size();
        }

        public boolean contains(String agentType) {
            return agents.containsKey(agentType);
        }
    }

    /**
     * Agent 编排服务实现
     */
    private static class AgentOrchestrationServiceImpl implements AgentOrchestrationService {

        private final AgentRegistry registry;
        private final AgentAiService aiService;
        private final ObjectMapper objectMapper = new ObjectMapper();

        public AgentOrchestrationServiceImpl(AgentRegistry registry, AgentAiService aiService) {
            this.registry = registry;
            this.aiService = aiService;
        }

        @Override
        public AgentExecutionResult executeAgent(String agentType, AgentExecutionContext context) {
            BaseAgent agent = registry.getAgent(agentType);
            context.setAiService(aiService);
            return agent.execute(context);
        }

        @Override
        public List<AgentExecutionResult> executeCodeReview(AgentExecutionContext context) {
            context.setAiService(aiService);

            List<String> inspectors = List.of(
                    "CODE_STANDARDS_INSPECTOR",
                    "ARCHITECTURE_GUARDIAN",
                    "SECURITY_AUDITOR"
            );

            return inspectors.parallelStream()
                    .map(agentType -> executeAgent(agentType, context))
                    .toList();
        }

        @Override
        public List<BaseAgent> getAllAgents() {
            return registry.getAllAgents();
        }

        @Override
        public boolean hasAgent(String agentType) {
            return registry.contains(agentType);
        }

        @Override
        public com.codereview.ai.domain.model.TeachingReport generateTeachingReport(
                Long reviewId,
                String codeContent,
                String language,
                String fileName,
                Long userId,
                int totalIssues,
                int criticalIssues,
                int highIssues,
                int mediumIssues,
                int lowIssues
        ) {
            // 构建教学报告生成的上下文
            AgentExecutionContext context = AgentExecutionContext.builder()
                    .requestId("teaching-report-" + reviewId)
                    .userId(userId)
                    .code(codeContent)
                    .language(language)
                    .filePath(fileName)
                    .build();

            // 设置模式为教学报告生成
            context.addContextData("mode", "teaching_report");
            context.addContextData("totalIssues", totalIssues);
            context.addContextData("criticalIssues", criticalIssues);
            context.addContextData("highIssues", highIssues);
            context.addContextData("mediumIssues", mediumIssues);
            context.addContextData("lowIssues", lowIssues);

            // 执行 TeachingMentor 智能体
            AgentExecutionResult result = executeAgent("TEACHING_MENTOR", context);

            if (!result.isSuccess()) {
                log.warn("Failed to generate teaching report for reviewId={}: {}", reviewId, result.getMessage());
                return null;
            }

            // 从执行结果中提取数据，构建 TeachingReport 实体
            return buildTeachingReport(result, reviewId, codeContent, language, userId,
                    totalIssues, criticalIssues, highIssues, mediumIssues, lowIssues);
        }

        /**
         * 根据智能体执行结果构建教学报告实体
         */
        private com.codereview.ai.domain.model.TeachingReport buildTeachingReport(
                AgentExecutionResult result,
                Long reviewId,
                String codeContent,
                String language,
                Long userId,
                int totalIssues,
                int criticalIssues,
                int highIssues,
                int mediumIssues,
                int lowIssues
        ) {
            try {
                return com.codereview.ai.domain.model.TeachingReport.builder()
                        .reviewId(reviewId)
                        .language(language)
                        .codeSummary(generateCodeSummary(codeContent))
                        .summary(result.getOutput("summary", "代码审查完成"))
                        .knowledgeGapsJson(formatJsonList(result.getOutput("knowledgeGaps")))
                        .keyFindingsJson(result.getOutput("keyFindings", "[]"))
                        .learningResourcesJson(result.getOutput("learningResources", "{}"))
                        .priorityActionsJson(formatJsonList(result.getOutput("priorityActions")))
                        .encouragement(result.getOutput("encouragement", ""))
                        .totalIssues(totalIssues)
                        .criticalIssues(criticalIssues)
                        .highIssues(highIssues)
                        .mediumIssues(mediumIssues)
                        .lowIssues(lowIssues)
                        .build();
            } catch (Exception e) {
                log.error("Failed to build teaching report from agent result", e);
                return null;
            }
        }

        /**
         * 生成代码摘要
         */
        private String generateCodeSummary(String codeContent) {
            if (codeContent == null || codeContent.isEmpty()) {
                return "无代码内容";
            }

            // 简单统计代码行数和主要结构
            int lines = codeContent.split("\n").length;
            int methods = codeContent.split("void|int|String|boolean|public|private|protected").length - 1;
            int classes = codeContent.split("class|interface|enum").length - 1;

            return String.format("代码约%d行，包含%d个方法定义，%d个类/接口", lines, methods, classes);
        }

        /**
         * 格式化列表为JSON数组字符串
         */
        @SuppressWarnings("unchecked")
        private String formatJsonList(Object output) {
            if (output == null) {
                return "[]";
            }
            if (output instanceof String) {
                return "[" + ((String) output).replace(", ", "\",\"") + "]";
            }
            if (output instanceof List) {
                try {
                    return objectMapper.writeValueAsString(output);
                } catch (Exception e) {
                    return "[]";
                }
            }
            return "[]";
        }
    }
}
