package com.codereview.ai.domain.service.impl;

import com.codereview.ai.domain.agent.shared.AgentExecutionContext;
import com.codereview.ai.domain.agent.shared.AgentExecutionResult;
import com.codereview.ai.domain.agent.shared.AgentOrchestrationService;
import com.codereview.ai.domain.agent.shared.mentor.SkillAssessor;
import com.codereview.ai.domain.agent.shared.mentor.LearningPathPlanner;
import com.codereview.ai.domain.model.*;
import com.codereview.ai.domain.repository.*;
import com.codereview.ai.domain.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Agent 服务实现
 * 封装 Agent 执行的业务逻辑
 *
 * @author AI Code Mentor Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final AgentOrchestrationService agentService;
    private final CodeReviewRepository codeReviewRepository;
    private final CodeIssueRepository codeIssueRepository;
    private final UserRepository userRepository;
    private final SkillProfileRepository skillProfileRepository;
    private final ExerciseRepository exerciseRepository;

    // ==================== 代码审查功能 ====================

    @Override
    @Transactional
    public CodeReview executeCodeReview(Long userId, Long projectId, String code, String language, String filePath) {
        log.info("Starting code review: userId={}, projectId={}, language={}", userId, projectId, language);

        // 创建代码审查记录
        CodeReview review = CodeReview.builder()
                .userId(userId)
                .codeContent(code)
                .language(language)
                .fileName(filePath)
                .status(CodeReview.ReviewStatus.PROCESSING)
                .build();

        review = codeReviewRepository.save(review);

        try {
            // 构建执行上下文
            AgentExecutionContext context = AgentExecutionContext.builder()
                    .requestId("review-" + review.getId())
                    .userId(userId)
                    .code(code)
                    .language(language)
                    .filePath(filePath)
                    .projectId(projectId)
                    .build();

            // 执行代码审查
            List<AgentExecutionResult> results = agentService.executeCodeReview(context);

            // 处理结果并保存
            processCodeReviewResults(review, results);

            review.setStatus(CodeReview.ReviewStatus.COMPLETED);

            log.info("Code review completed: reviewId={}, issues found={}",
                    review.getId(), review.getTotalIssues());

        } catch (Exception e) {
            log.error("Code review failed", e);
            review.setStatus(CodeReview.ReviewStatus.FAILED);
        }

        return codeReviewRepository.save(review);
    }

    private void processCodeReviewResults(CodeReview review, List<AgentExecutionResult> results) {
        List<CodeIssue> allIssues = new ArrayList<>();

        for (AgentExecutionResult result : results) {
            if (result.isSuccess() && result.getIssues() != null) {
                for (AgentExecutionResult.AgentIssue agentIssue : result.getIssues()) {
                    CodeIssue issue = convertAgentIssueToCodeIssue(agentIssue, review);
                    allIssues.add(issue);
                }
            }
        }

        // 按严重程度排序
        allIssues.sort((a, b) -> {
            int severityCompare = b.getSeverity().compareTo(a.getSeverity());
            if (severityCompare != 0) return severityCompare;
            return Integer.compare(
                    a.getLineNumber() != null ? a.getLineNumber() : Integer.MAX_VALUE,
                    b.getLineNumber() != null ? b.getLineNumber() : Integer.MAX_VALUE
            );
        });

        // 保存问题
        codeIssueRepository.saveAll(allIssues);

        // 更新审查统计
        review.setTotalIssues(allIssues.size());
    }

    private CodeIssue convertAgentIssueToCodeIssue(AgentExecutionResult.AgentIssue agentIssue, CodeReview review) {
        return CodeIssue.builder()
                .reviewId(review.getId())
                .title(agentIssue.getTitle())
                .description(agentIssue.getDescription())
                .severity(convertSeverity(agentIssue.getSeverity()))
                .category(agentIssue.getCategory())
                .lineNumber(agentIssue.getLineNumber())
                .codeSnippet(agentIssue.getCodeSnippet())
                .suggestion(agentIssue.getSuggestion())
                .teachingExplanation(agentIssue.getTeachingExplanation())
                .agentType(agentIssue.getAgentType())
                .build();
    }

    private CodeIssue.Severity convertSeverity(AgentExecutionResult.Severity severity) {
        return switch (severity) {
            case CRITICAL -> CodeIssue.Severity.CRITICAL;
            case HIGH -> CodeIssue.Severity.HIGH;
            case MEDIUM -> CodeIssue.Severity.MEDIUM;
            case LOW -> CodeIssue.Severity.LOW;
            case INFO -> CodeIssue.Severity.LOW;
        };
    }

    // ==================== 技能评估功能 ====================

    @Override
    @Transactional
    public SkillAssessmentResult assessSkills(Long userId, String code, String language) {
        log.info("Assessing skills: userId={}, language={}", userId, language);

        AgentExecutionContext context = AgentExecutionContext.builder()
                .requestId("skill-" + userId + "-" + System.currentTimeMillis())
                .userId(userId)
                .code(code)
                .language(language)
                .build();

        AgentExecutionResult result = agentService.executeAgent("SKILL_ASSESSOR", context);

        if (!result.isSuccess()) {
            throw new RuntimeException("Skill assessment failed: " + result.getError());
        }

        // 获取输出数据
        Map<String, Object> output = result.getOutputData();
        Integer overallScore = (Integer) output.getOrDefault("overallScore", 50);
        String overallLevel = (String) output.getOrDefault("overallLevel", "INTERMEDIATE");

        // 保存技能档案
        SkillProfile profile = SkillProfile.builder()
                .userId(userId)
                .language(language)
                .category("OVERALL")
                .skillLevel(overallScore)
                .lastAssessedAt(LocalDateTime.now())
                .build();

        skillProfileRepository.save(profile);

        // 构建返回结果
        List<String> strengths = (List<String>) output.getOrDefault("strengths", new ArrayList<>());
        List<String> improvements = (List<String>) output.getOrDefault("improvements", new ArrayList<>());
        List<Map<String, Object>> recommendationsRaw = (List<Map<String, Object>>) output.getOrDefault("recommendations", new ArrayList<>());

        List<SkillAssessor.SkillRecommendation> recommendations = new ArrayList<>();
        for (Map<String, Object> rec : recommendationsRaw) {
            recommendations.add(SkillAssessor.SkillRecommendation.builder()
                    .topic((String) rec.get("topic"))
                    .priority((String) rec.getOrDefault("priority", "MEDIUM"))
                    .reason((String) rec.getOrDefault("reason", ""))
                    .build());
        }

        return new SkillAssessmentResult(userId, language, overallScore, overallLevel, strengths, improvements, recommendations);
    }

    // ==================== 教学功能 ====================

    @Override
    public TeachingContent generateTeachingContent(Long userId, String topic, String userLevel, String language) {
        log.info("Generating teaching content: userId={}, topic={}, level={}", userId, topic, userLevel);

        AgentExecutionContext context = AgentExecutionContext.builder()
                .requestId("teaching-" + userId + "-" + System.currentTimeMillis())
                .userId(userId)
                .language(language)
                .contextData(Map.of(
                        "topic", topic,
                        "userLevel", userLevel
                ))
                .build();

        AgentExecutionResult result = agentService.executeAgent("TEACHING_MENTOR", context);

        if (!result.isSuccess()) {
            throw new RuntimeException("Teaching content generation failed: " + result.getError());
        }

        String formattedContent = (String) result.getOutputData().get("formattedContent");
        String nextSteps = result.getOutputData().getOrDefault("nextSteps", "[]").toString();

        return new TeachingContent(userId, topic, userLevel, language, formattedContent, nextSteps, LocalDateTime.now());
    }

    // ==================== 练习生成功能 ====================

    @Override
    @Transactional
    public Exercise generateExercise(Long userId, String language, String difficulty, String type, String topic) {
        log.info("Generating exercise: userId={}, language={}, difficulty={}, type={}",
                userId, language, difficulty, type);

        AgentExecutionContext context = AgentExecutionContext.builder()
                .requestId("exercise-" + userId + "-" + System.currentTimeMillis())
                .userId(userId)
                .language(language)
                .contextData(Map.of(
                        "difficulty", difficulty,
                        "type", type,
                        "topic", topic != null ? topic : ""
                ))
                .build();

        AgentExecutionResult result = agentService.executeAgent("EXERCISE_COACH", context);

        if (!result.isSuccess()) {
            throw new RuntimeException("Exercise generation failed: " + result.getError());
        }

        String exerciseJson = (String) result.getOutputData().get("exercise");
        Map<String, Object> exerciseData = parseJsonToMap(exerciseJson);

        // 解析测试用例
        List<Map<String, Object>> testCasesRaw = (List<Map<String, Object>>) exerciseData.getOrDefault("testCases", new ArrayList<>());
        List<Exercise.TestCase> testCases = new ArrayList<>();
        for (Map<String, Object> tc : testCasesRaw) {
            testCases.add(Exercise.TestCase.builder()
                    .name((String) tc.get("name"))
                    .input((String) tc.get("input"))
                    .expectedOutput((String) tc.get("expectedOutput"))
                    .isHidden((Boolean) tc.getOrDefault("isHidden", false))
                    .build());
        }

        // 创建练习题
        Exercise exercise = Exercise.builder()
                .title((String) exerciseData.get("title"))
                .description((String) exerciseData.get("description"))
                .language(language)
                .difficulty(Exercise.Difficulty.valueOf(difficulty))
                .category(type) // Use category instead of type
                .starterCode((String) exerciseData.get("starterCode"))
                .solutionCode((String) exerciseData.get("solution"))
                .testCases(testCases)
                .hints((List<String>) exerciseData.getOrDefault("hints", new ArrayList<>()))
                .estimatedMinutes((Integer) exerciseData.getOrDefault("estimatedTime", 30))
                .isPublished(false)
                .creatorId(userId)
                .build();

        return exerciseRepository.save(exercise);
    }

    // ==================== 学习路径规划功能 ====================

    @Override
    public LearningPath generateLearningPath(Long userId, String targetSkill, String currentLevel, String description, String language) {
        log.info("Generating learning path: userId={}, targetSkill={}, currentLevel={}",
                userId, targetSkill, currentLevel);

        AgentExecutionContext context = AgentExecutionContext.builder()
                .requestId("path-" + userId + "-" + System.currentTimeMillis())
                .userId(userId)
                .language(language)
                .contextData(Map.of(
                        "targetSkill", targetSkill,
                        "currentLevel", currentLevel,
                        "description", description != null ? description : ""
                ))
                .build();

        AgentExecutionResult result = agentService.executeAgent("LEARNING_PATH_PLANNER", context);

        if (!result.isSuccess()) {
            throw new RuntimeException("Learning path generation failed: " + result.getError());
        }

        String pathJson = (String) result.getOutputData().get("learningPath");
        Map<String, Object> pathData = parseJsonToMap(pathJson);

        // 解析模块
        List<Map<String, Object>> modulesRaw = (List<Map<String, Object>>) pathData.getOrDefault("modules", new ArrayList<>());
        List<LearningPathPlanner.ModuleSummary> modules = new ArrayList<>();
        for (Map<String, Object> mod : modulesRaw) {
            modules.add(LearningPathPlanner.ModuleSummary.builder()
                    .order(((Number) mod.get("order")).intValue())
                    .title((String) mod.get("title"))
                    .stage((String) mod.get("stage"))
                    .duration(((Number) mod.get("duration")).intValue())
                    .build());
        }

        // 解析里程碑
        List<Map<String, Object>> milestonesRaw = (List<Map<String, Object>>) pathData.getOrDefault("milestones", new ArrayList<>());
        List<String> milestones = new ArrayList<>();
        for (Map<String, Object> milestone : milestonesRaw) {
            milestones.add(milestone.get("title") + ": " + milestone.get("description"));
        }

        return new LearningPath(userId, targetSkill, currentLevel,
                (String) pathData.get("title"),
                (String) pathData.get("description"),
                ((Number) pathData.getOrDefault("estimatedDuration", 12)).intValue(),
                modules,
                milestones,
                LocalDateTime.now());
    }

    // ==================== 辅助方法 ====================

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonToMap(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse JSON", e);
            return new HashMap<>();
        }
    }
}
