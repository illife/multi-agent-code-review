package com.company.kb.core.service.impl;

import com.company.kb.core.domain.AgentTask;
import com.company.kb.core.repository.AgentTaskRepository;
import com.company.kb.core.service.AgentTaskService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体任务服务实现
 * 处理后台异步任务的生命周期管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentTaskServiceImpl implements AgentTaskService {

    private final AgentTaskRepository agentTaskRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AgentTask createTask(
            String taskName,
            AgentTask.TaskType taskType,
            String userId,
            String config,
            Integer priority,
            String createdBy
    ) {
        log.info("创建智能体任务: taskName={}, taskType={}, userId={}", taskName, taskType, userId);

        AgentTask task = AgentTask.builder()
                .taskName(taskName)
                .taskType(taskType)
                .userId(userId)
                .config(config)
                .status(AgentTask.TaskStatus.PENDING)
                .priority(priority != null ? priority : 2)
                .retryCount(0)
                .maxRetries(3)
                .createdBy(createdBy)
                .build();

        return agentTaskRepository.save(task);
    }

    @Override
    @Transactional
    public AgentTask createScheduledTask(
            String taskName,
            AgentTask.TaskType taskType,
            String userId,
            String config,
            String cronExpression,
            String createdBy
    ) {
        log.info("创建定时智能体任务: taskName={}, taskType={}, cron={}", taskName, taskType, cronExpression);

        // 计算下次执行时间（简单实现：立即执行）
        LocalDateTime nextRunAt = calculateNextRunTime(cronExpression);

        AgentTask task = AgentTask.builder()
                .taskName(taskName)
                .taskType(taskType)
                .userId(userId)
                .config(config)
                .status(AgentTask.TaskStatus.PENDING)
                .priority(2)
                .cronExpression(cronExpression)
                .nextRunAt(nextRunAt)
                .retryCount(0)
                .maxRetries(3)
                .createdBy(createdBy)
                .build();

        return agentTaskRepository.save(task);
    }

    @Override
    @Transactional
    public AgentTask executeTask(Long taskId) {
        log.info("开始执行任务: taskId={}", taskId);

        AgentTask task = agentTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));

        if (task.getStatus() != AgentTask.TaskStatus.PENDING) {
            throw new IllegalStateException("任务状态不允许执行: " + task.getStatus());
        }

        markAsRunning(task);

        try {
            // 根据任务类型执行具体逻辑
            String result = executeTaskByType(task);

            markAsCompleted(task, result);
            log.info("任务执行成功: taskId={}, resultLength={}", taskId, result.length());

        } catch (Exception e) {
            log.error("任务执行失败: taskId={}", taskId, e);

            if (task.getRetryCount() < task.getMaxRetries()) {
                // 标记为可重试
                task.setRetryCount(task.getRetryCount() + 1);
                task.setStatus(AgentTask.TaskStatus.PENDING);
                agentTaskRepository.save(task);
                log.info("任务将重试: taskId={}, retryCount={}", taskId, task.getRetryCount());
            } else {
                markAsFailed(task, e.getMessage());
            }
        }

        return task;
    }

    @Override
    @Transactional
    public void markAsRunning(AgentTask task) {
        task.setStatus(AgentTask.TaskStatus.RUNNING);
        task.setStartedAt(LocalDateTime.now());
        agentTaskRepository.save(task);
    }

    @Override
    @Transactional
    public void markAsCompleted(AgentTask task, String result) {
        task.setStatus(AgentTask.TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        task.setResult(result);

        // 如果是定时任务，计算下次执行时间
        if (task.getCronExpression() != null && !task.getCronExpression().isEmpty()) {
            LocalDateTime nextRunAt = calculateNextRunTime(task.getCronExpression());
            if (nextRunAt != null) {
                task.setNextRunAt(nextRunAt);
                task.setStatus(AgentTask.TaskStatus.PENDING);
            }
        }

        agentTaskRepository.save(task);
    }

    @Override
    @Transactional
    public void markAsFailed(AgentTask task, String errorMessage) {
        task.setStatus(AgentTask.TaskStatus.FAILED);
        task.setCompletedAt(LocalDateTime.now());
        task.setErrorMessage(errorMessage != null && errorMessage.length() > 1000
                ? errorMessage.substring(0, 1000)
                : errorMessage);
        agentTaskRepository.save(task);
    }

    @Override
    @Transactional
    public AgentTask retryTask(Long taskId) {
        AgentTask task = agentTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));

        if (task.getStatus() != AgentTask.TaskStatus.FAILED) {
            throw new IllegalStateException("只能重试失败的任务");
        }

        task.setStatus(AgentTask.TaskStatus.PENDING);
        task.setRetryCount(task.getRetryCount() + 1);
        task.setErrorMessage(null);

        return agentTaskRepository.save(task);
    }

    @Override
    @Transactional
    public void cancelTask(Long taskId, String userId) {
        AgentTask task = agentTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));

        if (!task.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权取消此任务");
        }

        if (task.getStatus() == AgentTask.TaskStatus.RUNNING) {
            throw new IllegalStateException("无法取消正在执行的任务");
        }

        task.setStatus(AgentTask.TaskStatus.CANCELLED);
        agentTaskRepository.save(task);
    }

    @Override
    public Page<AgentTask> getUserTasks(String userId, Pageable pageable) {
        return agentTaskRepository.findByUserId(userId, pageable);
    }

    @Override
    public List<AgentTask> getPendingTasks() {
        return agentTaskRepository.findPendingTasks(LocalDateTime.now());
    }

    @Override
    public List<AgentTask> getTimeoutTasks(LocalDateTime timeout) {
        return agentTaskRepository.findTimeoutTasks(timeout);
    }

    @Override
    public List<AgentTask> getRetryableTasks() {
        return agentTaskRepository.findRetryableTasks();
    }

    @Override
    @Transactional
    public void handleTimeoutTask(AgentTask task) {
        log.warn("处理超时任务: taskId={}", task.getId());

        if (task.getRetryCount() < task.getMaxRetries()) {
            task.setStatus(AgentTask.TaskStatus.PENDING);
            task.setRetryCount(task.getRetryCount() + 1);
            agentTaskRepository.save(task);
        } else {
            markAsFailed(task, "任务执行超时");
        }
    }

    @Override
    public AgentTask getTaskById(Long taskId) {
        return agentTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));
    }

    @Override
    public Page<AgentTask> searchTasks(String userId, String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return agentTaskRepository.findByUserId(userId, pageable);
        }

        return agentTaskRepository.searchByKeyword(userId, keyword, pageable);
    }

    @Override
    public long countTasksByStatus(String userId, AgentTask.TaskStatus status) {
        return agentTaskRepository.countByUserIdAndStatus(userId, status);
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId, String userId) {
        AgentTask task = agentTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("任务不存在: " + taskId));

        if (!task.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除此任务");
        }

        agentTaskRepository.delete(task);
    }

    @Override
    @Transactional
    public void updateNextRunTime(AgentTask task, LocalDateTime nextRunAt) {
        task.setNextRunAt(nextRunAt);
        agentTaskRepository.save(task);
    }

    /**
     * 根据任务类型执行具体任务逻辑
     */
    private String executeTaskByType(AgentTask task) throws Exception {
        return switch (task.getTaskType()) {
            case KNOWLEDGE_EXTRACTION -> executeKnowledgeExtraction(task);
            case CONTENT_GENERATION -> executeContentGeneration(task);
            case DOCUMENT_ANALYSIS -> executeDocumentAnalysis(task);
            case LEARNING_PATH -> executeLearningPath(task);
            case KNOWLEDGE_GAP_ANALYSIS -> executeKnowledgeGapAnalysis(task);
            case CUSTOM -> executeCustomTask(task);
            default -> throw new IllegalArgumentException("不支持的任务类型: " + task.getTaskType());
        };
    }

    /**
     * 执行知识提取任务
     */
    private String executeKnowledgeExtraction(AgentTask task) throws JsonProcessingException {
        log.info("执行知识提取任务: taskId={}", task.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getId());
        result.put("taskType", "KNOWLEDGE_EXTRACTION");
        result.put("status", "completed");
        result.put("message", "知识提取任务执行成功");
        result.put("timestamp", LocalDateTime.now().toString());

        return objectMapper.writeValueAsString(result);
    }

    /**
     * 执行内容生成任务
     */
    private String executeContentGeneration(AgentTask task) throws JsonProcessingException {
        log.info("执行内容生成任务: taskId={}", task.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getId());
        result.put("taskType", "CONTENT_GENERATION");
        result.put("status", "completed");
        result.put("message", "内容生成任务执行成功");
        result.put("timestamp", LocalDateTime.now().toString());

        return objectMapper.writeValueAsString(result);
    }

    /**
     * 执行文档分析任务
     */
    private String executeDocumentAnalysis(AgentTask task) throws JsonProcessingException {
        log.info("执行文档分析任务: taskId={}", task.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getId());
        result.put("taskType", "DOCUMENT_ANALYSIS");
        result.put("status", "completed");
        result.put("message", "文档分析任务执行成功");
        result.put("timestamp", LocalDateTime.now().toString());

        return objectMapper.writeValueAsString(result);
    }

    /**
     * 执行学习路径规划任务
     */
    private String executeLearningPath(AgentTask task) throws JsonProcessingException {
        log.info("执行学习路径规划任务: taskId={}", task.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getId());
        result.put("taskType", "LEARNING_PATH");
        result.put("status", "completed");
        result.put("message", "学习路径规划任务执行成功");
        result.put("timestamp", LocalDateTime.now().toString());

        return objectMapper.writeValueAsString(result);
    }

    /**
     * 执行知识缺口分析任务
     */
    private String executeKnowledgeGapAnalysis(AgentTask task) throws JsonProcessingException {
        log.info("执行知识缺口分析任务: taskId={}", task.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getId());
        result.put("taskType", "KNOWLEDGE_GAP_ANALYSIS");
        result.put("status", "completed");
        result.put("message", "知识缺口分析任务执行成功");
        result.put("timestamp", LocalDateTime.now().toString());

        return objectMapper.writeValueAsString(result);
    }

    /**
     * 执行自定义任务
     */
    private String executeCustomTask(AgentTask task) throws JsonProcessingException {
        log.info("执行自定义任务: taskId={}", task.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getId());
        result.put("taskType", "CUSTOM");
        result.put("status", "completed");
        result.put("message", "自定义任务执行成功");
        result.put("timestamp", LocalDateTime.now().toString());

        return objectMapper.writeValueAsString(result);
    }

    /**
     * 计算下次执行时间（简化实现）
     * 实际生产环境应该使用成熟的Cron解析库如Quartz
     */
    private LocalDateTime calculateNextRunTime(String cronExpression) {
        // 简化实现：返回当前时间+1分钟
        // 生产环境应该使用CronSequenceGenerator或其他Cron库
        return LocalDateTime.now().plusMinutes(1);
    }
}
