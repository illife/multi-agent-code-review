package com.company.kb.core.service;

import com.company.kb.core.domain.AgentTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能体任务服务接口
 * 管理后台异步执行的智能体任务
 */
public interface AgentTaskService {

    /**
     * 创建新任务
     *
     * @param taskName 任务名称
     * @param taskType 任务类型
     * @param userId 用户ID
     * @param config 任务配置（JSON格式）
     * @param priority 优先级
     * @param createdBy 创建者
     * @return 创建的任务
     */
    AgentTask createTask(
            String taskName,
            AgentTask.TaskType taskType,
            String userId,
            String config,
            Integer priority,
            String createdBy
    );

    /**
     * 创建定时任务
     *
     * @param taskName 任务名称
     * @param taskType 任务类型
     * @param userId 用户ID
     * @param config 任务配置
     * @param cronExpression Cron表达式
     * @param createdBy 创建者
     * @return 创建的任务
     */
    AgentTask createScheduledTask(
            String taskName,
            AgentTask.TaskType taskType,
            String userId,
            String config,
            String cronExpression,
            String createdBy
    );

    /**
     * 执行任务
     *
     * @param taskId 任务ID
     * @return 执行结果
     */
    AgentTask executeTask(Long taskId);

    /**
     * 标记任务为执行中
     *
     * @param task 任务
     */
    void markAsRunning(AgentTask task);

    /**
     * 标记任务为完成
     *
     * @param task 任务
     * @param result 执行结果
     */
    void markAsCompleted(AgentTask task, String result);

    /**
     * 标记任务为失败
     *
     * @param task 任务
     * @param errorMessage 错误信息
     */
    void markAsFailed(AgentTask task, String errorMessage);

    /**
     * 重试失败任务
     *
     * @param taskId 任务ID
     * @return 重试后的任务
     */
    AgentTask retryTask(Long taskId);

    /**
     * 取消任务
     *
     * @param taskId 任务ID
     * @param userId 用户ID
     */
    void cancelTask(Long taskId, String userId);

    /**
     * 获取用户任务列表
     *
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 任务列表
     */
    Page<AgentTask> getUserTasks(String userId, Pageable pageable);

    /**
     * 获取待执行任务列表
     *
     * @return 待执行任务列表
     */
    List<AgentTask> getPendingTasks();

    /**
     * 获取超时任务列表
     *
     * @param timeout 超时时间阈值
     * @return 超时任务列表
     */
    List<AgentTask> getTimeoutTasks(LocalDateTime timeout);

    /**
     * 获取可重试的失败任务
     *
     * @return 可重试任务列表
     */
    List<AgentTask> getRetryableTasks();

    /**
     * 处理超时任务
     *
     * @param task 超时任务
     */
    void handleTimeoutTask(AgentTask task);

    /**
     * 获取任务详情
     *
     * @param taskId 任务ID
     * @return 任务详情
     */
    AgentTask getTaskById(Long taskId);

    /**
     * 搜索任务
     *
     * @param userId 用户ID
     * @param keyword 关键词
     * @param pageable 分页参数
     * @return 搜索结果
     */
    Page<AgentTask> searchTasks(String userId, String keyword, Pageable pageable);

    /**
     * 统计任务执行情况
     *
     * @param userId 用户ID
     * @param status 任务状态
     * @return 任务数量
     */
    long countTasksByStatus(String userId, AgentTask.TaskStatus status);

    /**
     * 删除任务
     *
     * @param taskId 任务ID
     * @param userId 用户ID
     */
    void deleteTask(Long taskId, String userId);

    /**
     * 更新任务下次执行时间
     *
     * @param task 任务
     * @param nextRunAt 下次执行时间
     */
    void updateNextRunTime(AgentTask task, LocalDateTime nextRunAt);
}
