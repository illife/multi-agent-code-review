package com.company.kb.controller;

import com.company.kb.api.dto.PageResponse;
import com.company.kb.core.domain.AgentTask;
import com.company.kb.core.service.AgentTaskService;
import com.think.platform.shared.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体任务控制器
 * 提供智能体任务创建、查询、管理API
 */
@Slf4j
@RestController
@RequestMapping("/agent-tasks")
@RequiredArgsConstructor
public class AgentTaskController {

    private final AgentTaskService agentTaskService;

    /**
     * 创建新任务
     *
     * @param request 创建请求
     * @return 创建的任务
     */
    @PostMapping("/create")
    public Result<AgentTask> createTask(
            @RequestBody CreateTaskRequest request,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            log.info("创建智能体任务: username={}, taskName={}, taskType={}",
                    username, request.getTaskName(), request.getTaskType());

            AgentTask task = agentTaskService.createTask(
                    request.getTaskName(),
                    request.getTaskType(),
                    username,
                    request.getConfig(),
                    request.getPriority(),
                    username
            );

            return Result.success(task);

        } catch (Exception e) {
            log.error("创建智能体任务失败", e);
            return Result.failed(500, "创建智能体任务失败: " + e.getMessage());
        }
    }

    /**
     * 创建定时任务
     *
     * @param request 创建请求
     * @return 创建的任务
     */
    @PostMapping("/scheduled")
    public Result<AgentTask> createScheduledTask(
            @RequestBody ScheduledTaskRequest request,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            log.info("创建定时智能体任务: username={}, taskName={}, cron={}",
                    username, request.getTaskName(), request.getCronExpression());

            AgentTask task = agentTaskService.createScheduledTask(
                    request.getTaskName(),
                    request.getTaskType(),
                    username,
                    request.getConfig(),
                    request.getCronExpression(),
                    username
            );

            return Result.success(task);

        } catch (Exception e) {
            log.error("创建定时任务失败", e);
            return Result.failed(500, "创建定时任务失败: " + e.getMessage());
        }
    }

    /**
     * 手动执行任务
     *
     * @param taskId 任务ID
     * @return 执行结果
     */
    @PostMapping("/{taskId}/execute")
    @Async
    public Result<String> executeTask(@PathVariable Long taskId) {
        try {
            log.info("手动执行任务: taskId={}", taskId);
            agentTaskService.executeTask(taskId);
            return Result.success("任务已提交执行");

        } catch (IllegalArgumentException e) {
            return Result.failed(404, e.getMessage());
        } catch (IllegalStateException e) {
            return Result.failed(400, e.getMessage());
        } catch (Exception e) {
            log.error("执行任务失败: taskId={}", taskId, e);
            return Result.failed(500, "执行任务失败: " + e.getMessage());
        }
    }

    /**
     * 重试失败任务
     *
     * @param taskId 任务ID
     * @return 重试后的任务
     */
    @PostMapping("/{taskId}/retry")
    public Result<AgentTask> retryTask(@PathVariable Long taskId) {
        try {
            log.info("重试任务: taskId={}", taskId);
            AgentTask task = agentTaskService.retryTask(taskId);
            return Result.success(task);

        } catch (IllegalArgumentException e) {
            return Result.failed(404, e.getMessage());
        } catch (IllegalStateException e) {
            return Result.failed(400, e.getMessage());
        } catch (Exception e) {
            log.error("重试任务失败: taskId={}", taskId, e);
            return Result.failed(500, "重试任务失败: " + e.getMessage());
        }
    }

    /**
     * 取消任务
     *
     * @param taskId 任务ID
     * @return 取消结果
     */
    @PostMapping("/{taskId}/cancel")
    public Result<String> cancelTask(
            @PathVariable Long taskId,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            log.info("取消任务: taskId={}, username={}", taskId, username);
            agentTaskService.cancelTask(taskId, username);
            return Result.success("任务已取消");

        } catch (IllegalArgumentException e) {
            return Result.failed(404, e.getMessage());
        } catch (IllegalStateException e) {
            return Result.failed(400, e.getMessage());
        } catch (Exception e) {
            log.error("取消任务失败: taskId={}", taskId, e);
            return Result.failed(500, "取消任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户任务列表
     *
     * @param page 页码
     * @param size 每页大小
     * @return 任务列表
     */
    @GetMapping
    public Result<PageResponse<AgentTask>> getUserTasks(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<AgentTask> tasks = agentTaskService.getUserTasks(username, pageable);
            return Result.success(PageResponse.of(tasks));

        } catch (Exception e) {
            log.error("获取任务列表失败", e);
            return Result.failed(500, "获取任务列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务详情
     *
     * @param taskId 任务ID
     * @return 任务详情
     */
    @GetMapping("/{taskId}")
    public Result<AgentTask> getTask(@PathVariable Long taskId) {
        try {
            AgentTask task = agentTaskService.getTaskById(taskId);
            return Result.success(task);

        } catch (IllegalArgumentException e) {
            return Result.failed(404, e.getMessage());
        } catch (Exception e) {
            log.error("获取任务详情失败: taskId={}", taskId, e);
            return Result.failed(500, "获取任务详情失败: " + e.getMessage());
        }
    }

    /**
     * 搜索任务
     *
     * @param keyword 关键词
     * @param page 页码
     * @param size 每页大小
     * @return 搜索结果
     */
    @GetMapping("/search")
    public Result<PageResponse<AgentTask>> searchTasks(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<AgentTask> tasks = agentTaskService.searchTasks(username, keyword, pageable);
            return Result.success(PageResponse.of(tasks));

        } catch (Exception e) {
            log.error("搜索任务失败: keyword={}", keyword, e);
            return Result.failed(500, "搜索任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务统计信息
     *
     * @return 统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(Authentication authentication) {
        try {
            String username = authentication.getName();

            Map<String, Object> stats = new HashMap<>();
            stats.put("pending", agentTaskService.countTasksByStatus(username, AgentTask.TaskStatus.PENDING));
            stats.put("running", agentTaskService.countTasksByStatus(username, AgentTask.TaskStatus.RUNNING));
            stats.put("completed", agentTaskService.countTasksByStatus(username, AgentTask.TaskStatus.COMPLETED));
            stats.put("failed", agentTaskService.countTasksByStatus(username, AgentTask.TaskStatus.FAILED));
            stats.put("cancelled", agentTaskService.countTasksByStatus(username, AgentTask.TaskStatus.CANCELLED));

            return Result.success(stats);

        } catch (Exception e) {
            log.error("获取任务统计失败", e);
            return Result.failed(500, "获取任务统计失败: " + e.getMessage());
        }
    }

    /**
     * 获取待执行任务列表（管理员）
     *
     * @return 待执行任务列表
     */
    @GetMapping("/pending")
    public Result<List<AgentTask>> getPendingTasks() {
        try {
            List<AgentTask> tasks = agentTaskService.getPendingTasks();
            return Result.success(tasks);

        } catch (Exception e) {
            log.error("获取待执行任务失败", e);
            return Result.failed(500, "获取待执行任务失败: " + e.getMessage());
        }
    }

    /**
     * 删除任务
     *
     * @param taskId 任务ID
     * @return 删除结果
     */
    @DeleteMapping("/{taskId}")
    public Result<String> deleteTask(
            @PathVariable Long taskId,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            agentTaskService.deleteTask(taskId, username);
            return Result.success("任务删除成功");

        } catch (IllegalArgumentException e) {
            return Result.failed(404, e.getMessage());
        } catch (Exception e) {
            log.error("删除任务失败: taskId={}", taskId, e);
            return Result.failed(500, "删除任务失败: " + e.getMessage());
        }
    }

    /**
     * 创建任务请求
     */
    public static class CreateTaskRequest {
        private String taskName;
        private AgentTask.TaskType taskType;
        private String config;
        private Integer priority;

        public String getTaskName() {
            return taskName;
        }

        public void setTaskName(String taskName) {
            this.taskName = taskName;
        }

        public AgentTask.TaskType getTaskType() {
            return taskType;
        }

        public void setTaskType(AgentTask.TaskType taskType) {
            this.taskType = taskType;
        }

        public String getConfig() {
            return config;
        }

        public void setConfig(String config) {
            this.config = config;
        }

        public Integer getPriority() {
            return priority;
        }

        public void setPriority(Integer priority) {
            this.priority = priority;
        }
    }

    /**
     * 定时任务请求
     */
    public static class ScheduledTaskRequest {
        private String taskName;
        private AgentTask.TaskType taskType;
        private String config;
        private String cronExpression;

        public String getTaskName() {
            return taskName;
        }

        public void setTaskName(String taskName) {
            this.taskName = taskName;
        }

        public AgentTask.TaskType getTaskType() {
            return taskType;
        }

        public void setTaskType(AgentTask.TaskType taskType) {
            this.taskType = taskType;
        }

        public String getConfig() {
            return config;
        }

        public void setConfig(String config) {
            this.config = config;
        }

        public String getCronExpression() {
            return cronExpression;
        }

        public void setCronExpression(String cronExpression) {
            this.cronExpression = cronExpression;
        }
    }
}
