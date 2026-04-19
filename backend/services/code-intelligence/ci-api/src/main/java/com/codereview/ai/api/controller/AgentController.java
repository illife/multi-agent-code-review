package com.codereview.ai.api.controller;

import com.codereview.ai.domain.model.AgentExecution;
import com.codereview.ai.domain.model.AgentMessage;
import com.codereview.ai.domain.model.AgentTask;
import com.codereview.ai.domain.service.AgentOrchestratorService;
import com.codereview.ai.domain.service.AgentTaskService;
import com.think.platform.shared.common.dto.UserDTO;
import com.think.platform.shared.common.result.Result;
import com.think.platform.shared.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Agent Controller - Code Intelligence Service
 *
 * Multi-agent task management API
 *
 * @author Code Intelligence Service Team
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Tag(name = "多智能体任务", description = "多智能体任务管理API")
public class AgentController {

    private final AgentTaskService taskService;
    private final AgentOrchestratorService orchestratorService;

    /**
     * Create code review task
     * POST /api/agent/tasks/code-review
     */
    @PostMapping("/tasks/code-review")
    @Operation(summary = "创建代码审查任务", description = "上传项目代码并创建AI审查任务")
    public Result<AgentTask> createCodeReviewTask(
            @Parameter(description = "审查请求参数") @Valid @RequestBody CodeReviewRequest request) {
        Long userId = getCurrentUserId();
        AgentTask task = taskService.createCodeReviewTask(userId, request.getProjectId());
        return Result.success(task);
    }

    /**
     * Create and execute code review task
     * POST /api/agent/tasks/code-review/execute
     */
    @PostMapping("/tasks/code-review/execute")
    @Operation(summary = "创建并执行代码审查任务", description = "创建代码审查任务并立即执行")
    public Result<AgentTask> executeCodeReviewTask(
            @Parameter(description = "审查请求参数") @Valid @RequestBody CodeReviewRequest request) {
        Long userId = getCurrentUserId();
        AgentTask task = taskService.createAndExecuteCodeReviewTask(userId, request.getProjectId());
        return Result.success(task);
    }

    /**
     * Create learning path task
     * POST /api/agent/tasks/learning-path
     */
    @PostMapping("/tasks/learning-path")
    @Operation(summary = "创建学习路径任务", description = "生成个性化的学习路径")
    public Result<AgentTask> createLearningPathTask(
            @Parameter(description = "学习路径请求参数") @Valid @RequestBody LearningPathRequest request) {
        Long userId = getCurrentUserId();
        AgentTask task = taskService.createLearningPathTask(
                userId, request.getTargetSkill(), request.getCurrentLevel(), request.getDescription());
        return Result.success(task);
    }

    /**
     * Create and execute learning path task
     * POST /api/agent/tasks/learning-path/execute
     */
    @PostMapping("/tasks/learning-path/execute")
    @Operation(summary = "创建并执行学习路径任务", description = "生成学习路径并立即返回结果")
    public Result<AgentTask> executeLearningPathTask(
            @Parameter(description = "学习路径请求参数") @Valid @RequestBody LearningPathRequest request) {
        Long userId = getCurrentUserId();
        AgentTask task = taskService.createAndExecuteLearningPathTask(
                userId, request.getTargetSkill(), request.getCurrentLevel(), request.getDescription());
        return Result.success(task);
    }

    /**
     * Create exercise generation task
     * POST /api/agent/tasks/exercise
     */
    @PostMapping("/tasks/exercise")
    @Operation(summary = "创建练习题生成任务", description = "生成编程练习题")
    public Result<AgentTask> createExerciseTask(
            @Parameter(description = "练习题请求参数") @Valid @RequestBody ExerciseRequest request) {
        Long userId = getCurrentUserId();
        AgentTask task = taskService.createExerciseTask(
                userId, request.getSkillTag(), request.getDifficulty(), request.getCount());
        return Result.success(task);
    }

    /**
     * Create and execute exercise generation task
     * POST /api/agent/tasks/exercise/execute
     */
    @PostMapping("/tasks/exercise/execute")
    @Operation(summary = "创建并执行练习题生成任务", description = "生成练习题并立即返回结果")
    public Result<AgentTask> executeExerciseTask(
            @Parameter(description = "练习题请求参数") @Valid @RequestBody ExerciseRequest request) {
        Long userId = getCurrentUserId();
        AgentTask task = taskService.createAndExecuteExerciseTask(
                userId, request.getSkillTag(), request.getDifficulty(), request.getCount());
        return Result.success(task);
    }

    /**
     * Create Q&A task
     * POST /api/agent/tasks/qa
     */
    @PostMapping("/tasks/qa")
    @Operation(summary = "创建问答任务", description = "提交问题并获得AI回答")
    public Result<AgentTask> createQATask(
            @Parameter(description = "问答请求参数") @Valid @RequestBody QARequest request) {
        Long userId = getCurrentUserId();
        AgentTask task = taskService.createAndExecuteQATask(userId, request.getQuestion(), request.getContext());
        return Result.success(task);
    }

    /**
     * Get task status
     * GET /api/agent/tasks/{taskId}
     */
    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "获取任务状态", description = "查询任务执行状态和结果")
    public Result<AgentTask> getTaskStatus(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        Long userId = getCurrentUserId();
        AgentTask task = taskService.getTaskForUser(taskId, userId);
        return Result.success(task);
    }

    /**
     * Get task execution details
     * GET /api/agent/tasks/{taskId}/executions
     */
    @GetMapping("/tasks/{taskId}/executions")
    @Operation(summary = "获取任务执行详情", description = "查询任务中各个Agent的执行记录")
    public Result<List<AgentExecution>> getTaskExecutions(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        Long userId = getCurrentUserId();
        taskService.getTaskForUser(taskId, userId); // Permission check
        List<AgentExecution> executions = orchestratorService.getTaskExecutions(taskId);
        return Result.success(executions);
    }

    /**
     * Get task messages
     * GET /api/agent/tasks/{taskId}/messages
     */
    @GetMapping("/tasks/{taskId}/messages")
    @Operation(summary = "获取任务消息", description = "查询Agent之间的通信消息")
    public Result<List<AgentMessage>> getTaskMessages(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        Long userId = getCurrentUserId();
        taskService.getTaskForUser(taskId, userId); // Permission check
        List<AgentMessage> messages = orchestratorService.getTaskMessages(taskId);
        return Result.success(messages);
    }

    /**
     * Get task statistics
     * GET /api/agent/tasks/{taskId}/statistics
     */
    @GetMapping("/tasks/{taskId}/statistics")
    @Operation(summary = "获取任务统计", description = "查询任务的执行统计信息")
    public Result<Map<String, Object>> getTaskStatistics(
            @Parameter(description = "任务ID") @PathVariable Long taskId) {
        Long userId = getCurrentUserId();
        taskService.getTaskForUser(taskId, userId); // Permission check
        Map<String, Object> stats = orchestratorService.getTaskStatistics(taskId);
        return Result.success(stats);
    }

    /**
     * Get user tasks
     * GET /api/agent/tasks
     */
    @GetMapping("/tasks")
    @Operation(summary = "获取用户任务列表", description = "查询当前用户的所有任务")
    public Result<List<AgentTask>> getUserTasks() {
        Long userId = getCurrentUserId();
        List<AgentTask> tasks = taskService.getUserTasks(userId);
        return Result.success(tasks);
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
     * Code Review Request
     */
    public static class CodeReviewRequest {
        private Long projectId;

        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }
    }

    /**
     * Learning Path Request
     */
    public static class LearningPathRequest {
        private String targetSkill;
        private String currentLevel;
        private String description;

        public String getTargetSkill() { return targetSkill; }
        public void setTargetSkill(String targetSkill) { this.targetSkill = targetSkill; }
        public String getCurrentLevel() { return currentLevel; }
        public void setCurrentLevel(String currentLevel) { this.currentLevel = currentLevel; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    /**
     * Exercise Request
     */
    public static class ExerciseRequest {
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
     * Q&A Request
     */
    public static class QARequest {
        private String question;
        private String context;

        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public String getContext() { return context; }
        public void setContext(String context) { this.context = context; }
    }
}
