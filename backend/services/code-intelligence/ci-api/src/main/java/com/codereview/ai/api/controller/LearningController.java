package com.codereview.ai.api.controller;

import com.codereview.ai.domain.model.*;
import com.codereview.ai.domain.service.LearningService;
import com.think.platform.shared.common.result.Result;
import com.think.platform.shared.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Learning Controller - Code Intelligence Service
 *
 * AI teaching functionality API
 *
 * @author Code Intelligence Service Team
 */
@Slf4j
@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
@Tag(name = "AI教学", description = "AI学习路径和练习题管理API")
public class LearningController {

    private final LearningService learningService;

    /**
     * Create or get learning path
     * POST /api/learning/paths
     */
    @PostMapping("/paths")
    @Operation(summary = "创建学习路径", description = "获取或创建个性化的学习路径")
    public Result<LearningPath> getOrCreateLearningPath(
            @Parameter(description = "学习路径请求参数") @Valid @RequestBody LearningPathRequest request) {
        Long userId = getCurrentUserId();
        LearningPath path = learningService.getOrCreateLearningPath(userId, request.getTargetSkill());
        return Result.success(path);
    }

    /**
     * Get learning path by ID
     * GET /api/learning/paths/{pathId}
     */
    @GetMapping("/paths/{pathId}")
    @Operation(summary = "获取学习路径详情", description = "查询学习路径的详细信息")
    public Result<LearningPath> getLearningPath(
            @Parameter(description = "学习路径ID") @PathVariable Long pathId) {
        LearningPath path = learningService.getLearningPath(pathId);
        return Result.success(path);
    }

    /**
     * Get user learning paths
     * GET /api/learning/paths
     */
    @GetMapping("/paths")
    @Operation(summary = "获取用户学习路径列表", description = "查询当前用户的所有学习路径")
    public Result<List<LearningPath>> getUserLearningPaths() {
        Long userId = getCurrentUserId();
        List<LearningPath> paths = learningService.getUserLearningPaths(userId);
        return Result.success(paths);
    }

    /**
     * Get active learning paths
     * GET /api/learning/paths/active
     */
    @GetMapping("/paths/active")
    @Operation(summary = "获取活跃学习路径", description = "查询当前正在进行的学习路径")
    public Result<List<LearningPath>> getActiveLearningPaths() {
        Long userId = getCurrentUserId();
        List<LearningPath> paths = learningService.getActiveLearningPaths(userId);
        return Result.success(paths);
    }

    /**
     * Update learning path progress
     * PUT /api/learning/paths/{pathId}/progress
     */
    @PutMapping("/paths/{pathId}/progress")
    @Operation(summary = "更新学习进度", description = "更新学习路径的进度")
    public Result<Void> updateProgress(
            @Parameter(description = "学习路径ID") @PathVariable Long pathId,
            @Parameter(description = "进度更新参数") @Valid @RequestBody ProgressUpdateRequest request) {
        learningService.updateLearningPathProgress(pathId, request.getProgress(), request.getCurrentStep());
        return Result.success();
    }

    /**
     * Get exercise by ID
     * GET /api/learning/exercises/{exerciseId}
     */
    @GetMapping("/exercises/{exerciseId}")
    @Operation(summary = "获取练习题详情", description = "查询练习题的详细信息")
    public Result<Exercise> getExercise(
            @Parameter(description = "练习题ID") @PathVariable Long exerciseId) {
        Exercise exercise = learningService.getExercise(exerciseId);
        return Result.success(exercise);
    }

    /**
     * Get exercises by skill
     * GET /api/learning/exercises
     */
    @GetMapping("/exercises")
    @Operation(summary = "获取练习题列表", description = "根据技能标签查询练习题")
    public Result<List<Exercise>> getExercises(
            @Parameter(description = "技能标签") @RequestParam(required = false) String skillTag,
            @Parameter(description = "难度级别") @RequestParam(required = false) String difficulty) {
        List<Exercise> exercises = learningService.getExercisesBySkill(skillTag, difficulty);
        return Result.success(exercises);
    }

    /**
     * Generate exercises
     * POST /api/learning/exercises/generate
     */
    @PostMapping("/exercises/generate")
    @Operation(summary = "生成练习题", description = "使用AI生成编程练习题")
    public Result<List<Exercise>> generateExercises(
            @Parameter(description = "练习题生成请求") @Valid @RequestBody ExerciseGenerateRequest request) {
        Long userId = getCurrentUserId();
        List<Exercise> exercises = learningService.generateExercises(
                userId, request.getSkillTag(), request.getDifficulty(), request.getCount());
        return Result.success(exercises);
    }

    /**
     * Submit exercise solution
     * POST /api/learning/exercises/{exerciseId}/submit
     */
    @PostMapping("/exercises/{exerciseId}/submit")
    @Operation(summary = "提交练习题", description = "提交代码并获取评分")
    public Result<ExerciseSubmission> submitExercise(
            @Parameter(description = "练习题ID") @PathVariable Long exerciseId,
            @Parameter(description = "代码提交请求") @Valid @RequestBody CodeSubmissionRequest request) {
        Long userId = getCurrentUserId();
        ExerciseSubmission submission = learningService.submitExercise(exerciseId, userId, request.getCode());
        return Result.success(submission);
    }

    /**
     * Get submission history
     * GET /api/learning/exercises/{exerciseId}/submissions
     */
    @GetMapping("/exercises/{exerciseId}/submissions")
    @Operation(summary = "获取提交历史", description = "查询练习题的提交记录")
    public Result<List<ExerciseSubmission>> getSubmissionHistory(
            @Parameter(description = "练习题ID") @PathVariable Long exerciseId) {
        Long userId = getCurrentUserId();
        List<ExerciseSubmission> submissions = learningService.getSubmissionHistory(userId, exerciseId);
        return Result.success(submissions);
    }

    /**
     * Get best submission
     * GET /api/learning/exercises/{exerciseId}/best
     */
    @GetMapping("/exercises/{exerciseId}/best")
    @Operation(summary = "获取最佳提交", description = "查询练习题的最高分提交")
    public Result<ExerciseSubmission> getBestSubmission(
            @Parameter(description = "练习题ID") @PathVariable Long exerciseId) {
        Long userId = getCurrentUserId();
        return learningService.getBestSubmission(userId, exerciseId)
                .map(Result::success)
                .orElse(Result.error("暂无提交记录"));
    }

    /**
     * Get user statistics
     * GET /api/learning/statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取学习统计", description = "查询用户的学习统计信息")
    public Result<Map<String, Object>> getUserStatistics() {
        Long userId = getCurrentUserId();
        Map<String, Object> stats = learningService.getUserStatistics(userId);
        return Result.success(stats);
    }

    /**
     * Get current user ID from security context
     */
    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new com.think.platform.shared.common.exception.AuthenticationException("未登录"));
    }

    // ============================================================
    // Request DTOs
    // ============================================================

    /**
     * Learning Path Request
     */
    public static class LearningPathRequest {
        private String targetSkill;

        public String getTargetSkill() { return targetSkill; }
        public void setTargetSkill(String targetSkill) { this.targetSkill = targetSkill; }
    }

    /**
     * Progress Update Request
     */
    public static class ProgressUpdateRequest {
        private Integer progress;
        private Integer currentStep;

        public Integer getProgress() { return progress; }
        public void setProgress(Integer progress) { this.progress = progress; }
        public Integer getCurrentStep() { return currentStep; }
        public void setCurrentStep(Integer currentStep) { this.currentStep = currentStep; }
    }

    /**
     * Exercise Generate Request
     */
    public static class ExerciseGenerateRequest {
        private String skillTag;
        private String difficulty;
        private Integer count;

        public String getSkillTag() { return skillTag; }
        public void setSkillTag(String skillTag) { this.skillTag = skillTag; }
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
    }

    /**
     * Code Submission Request
     */
    public static class CodeSubmissionRequest {
        private String code;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
}
