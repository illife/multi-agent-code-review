package com.codereview.ai.domain.service;

import com.codereview.ai.domain.agent.shared.mentor.ExerciseCoach;
import com.codereview.ai.domain.agent.shared.mentor.LearningPathPlanner;
import com.codereview.ai.domain.agent.shared.mentor.SkillAssessor;
import com.codereview.ai.domain.model.*;

/**
 * Agent 服务接口
 * 定义 Agent 相关的业务操作
 *
 * @author AI Code Mentor Team
 */
public interface AgentService {

    // ==================== 代码审查功能 ====================

    /**
     * 执行代码审查
     *
     * @param userId    用户 ID
     * @param projectId 项目 ID
     * @param code      代码内容
     * @param language  编程语言
     * @param filePath  文件路径 (可选)
     * @return 代码审查结果
     */
    CodeReview executeCodeReview(Long userId, Long projectId, String code, String language, String filePath);

    // ==================== 技能评估功能 ====================

    /**
     * 评估用户技能
     *
     * @param userId   用户 ID
     * @param code     代码内容
     * @param language 编程语言
     * @return 技能评估结果
     */
    SkillAssessmentResult assessSkills(Long userId, String code, String language);

    // ==================== 教学功能 ====================

    /**
     * 生成教学内容
     *
     * @param userId    用户 ID
     * @param topic     教学主题
     * @param userLevel 用户水平
     * @param language  编程语言
     * @return 教学内容
     */
    TeachingContent generateTeachingContent(Long userId, String topic, String userLevel, String language);

    // ==================== 练习生成功能 ====================

    /**
     * 生成练习题
     *
     * @param userId     用户 ID
     * @param language   编程语言
     * @param difficulty 难度级别
     * @param type       练习类型
     * @param topic      主题 (可选)
     * @return 练习题
     */
    Exercise generateExercise(Long userId, String language, String difficulty, String type, String topic);

    // ==================== 学习路径规划功能 ====================

    /**
     * 生成学习路径
     *
     * @param userId        用户 ID
     * @param targetSkill   目标技能
     * @param currentLevel  当前水平
     * @param description   描述 (可选)
     * @param language      编程语言
     * @return 学习路径
     */
    LearningPath generateLearningPath(Long userId, String targetSkill, String currentLevel,
                                       String description, String language);

    // ==================== DTO 类 ====================

    /**
     * 技能评估结果
     */
    record SkillAssessmentResult(
            Long userId,
            String language,
            Integer overallScore,
            String overallLevel,
            java.util.List<String> strengths,
            java.util.List<String> improvements,
            java.util.List<SkillAssessor.SkillRecommendation> recommendations
    ) {}

    /**
     * 教学内容
     */
    record TeachingContent(
            Long userId,
            String topic,
            String userLevel,
            String language,
            String content,
            String nextSteps,
            java.time.LocalDateTime generatedAt
    ) {}

    /**
     * 学习路径
     */
    record LearningPath(
            Long userId,
            String targetSkill,
            String currentLevel,
            String title,
            String description,
            Integer estimatedDuration,
            java.util.List<LearningPathPlanner.ModuleSummary> modules,
            java.util.List<String> milestones,
            java.time.LocalDateTime generatedAt
    ) {}
}
