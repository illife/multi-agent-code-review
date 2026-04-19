package com.company.kb.core.service;

import com.company.kb.core.domain.AgentTask;
import com.company.kb.core.repository.AgentTaskRepository;
import com.company.kb.core.service.impl.AgentTaskServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AgentTaskService 单元测试
 * 测试智能体任务管理功能的各个场景
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("智能体任务服务测试")
class AgentTaskServiceTest {

    @Mock
    private AgentTaskRepository agentTaskRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AgentTaskServiceImpl agentTaskService;

    private static final String TEST_USER_ID = "test-user";
    private static final String TEST_CREATOR = "system";

    @BeforeEach
    void setUp() {
        reset(agentTaskRepository, objectMapper);

        // 默认mock ObjectMapper行为
        try {
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"result\":\"success\"}");
        } catch (Exception e) {
            // ignore
        }
    }

    // ==================== 创建任务测试 ====================

    @Test
    @DisplayName("创建新任务 - 成功")
    void createTask_Success() {
        // Given
        String taskName = "知识提取任务";
        AgentTask.TaskType taskType = AgentTask.TaskType.KNOWLEDGE_EXTRACTION;
        String config = "{\"source\":\"document\"}";
        Integer priority = 1;

        when(agentTaskRepository.save(any(AgentTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AgentTask result = agentTaskService.createTask(
                taskName, taskType, TEST_USER_ID, config, priority, TEST_CREATOR
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTaskName()).isEqualTo(taskName);
        assertThat(result.getTaskType()).isEqualTo(taskType);
        assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(result.getStatus()).isEqualTo(AgentTask.TaskStatus.PENDING);
        assertThat(result.getPriority()).isEqualTo(priority);
        assertThat(result.getRetryCount()).isEqualTo(0);
        assertThat(result.getMaxRetries()).isEqualTo(3);

        verify(agentTaskRepository).save(any(AgentTask.class));
    }

    @Test
    @DisplayName("创建定时任务 - 成功")
    void createScheduledTask_Success() {
        // Given
        String taskName = "定时内容生成";
        AgentTask.TaskType taskType = AgentTask.TaskType.CONTENT_GENERATION;
        String config = "{\"template\":\"lesson\"}";
        String cronExpression = "0 0 * * *";

        AgentTask mockTask = createMockTask(taskName, taskType);
        mockTask.setCronExpression(cronExpression);
        mockTask.setNextRunAt(LocalDateTime.now().plusMinutes(1));

        when(agentTaskRepository.save(any(AgentTask.class))).thenReturn(mockTask);

        // When
        AgentTask result = agentTaskService.createScheduledTask(
                taskName, taskType, TEST_USER_ID, config, cronExpression, TEST_CREATOR
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTaskName()).isEqualTo(taskName);
        assertThat(result.getCronExpression()).isEqualTo(cronExpression);
        assertThat(result.getNextRunAt()).isNotNull();

        verify(agentTaskRepository).save(any(AgentTask.class));
    }

    // ==================== 执行任务测试 ====================

    @Test
    @DisplayName("执行任务 - 成功完成")
    void executeTask_Success() {
        // Given
        Long taskId = 1L;
        AgentTask task = createMockTask("测试任务", AgentTask.TaskType.KNOWLEDGE_EXTRACTION);
        task.setStatus(AgentTask.TaskStatus.PENDING);
        task.setCronExpression(null); // 确保不是定时任务
        task.setId(taskId);

        when(agentTaskRepository.findById(eq(taskId))).thenReturn(Optional.of(task));
        when(agentTaskRepository.save(any(AgentTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        AgentTask result = agentTaskService.executeTask(taskId);

        // Then
        assertThat(result.getStatus()).isEqualTo(AgentTask.TaskStatus.COMPLETED);
        assertThat(result.getStartedAt()).isNotNull();
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getResult()).isNotNull();

        verify(agentTaskRepository).findById(taskId);
        verify(agentTaskRepository, atLeastOnce()).save(any(AgentTask.class));
    }

    @Test
    @DisplayName("执行任务 - 任务不存在")
    void executeTask_NotFound() {
        // Given
        Long taskId = 999L;
        when(agentTaskRepository.findById(eq(taskId))).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> agentTaskService.executeTask(taskId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不存在");

        verify(agentTaskRepository).findById(taskId);
        verify(agentTaskRepository, never()).save(any());
    }

    @Test
    @DisplayName("执行任务 - 状态不允许执行")
    void executeTask_InvalidStatus() {
        // Given
        Long taskId = 1L;
        AgentTask task = createMockTask("测试任务", AgentTask.TaskType.KNOWLEDGE_EXTRACTION);
        task.setStatus(AgentTask.TaskStatus.RUNNING); // 已在运行中
        task.setId(taskId);

        when(agentTaskRepository.findById(eq(taskId))).thenReturn(Optional.of(task));

        // When & Then
        assertThatThrownBy(() -> agentTaskService.executeTask(taskId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("不允许");
    }

    @Test
    @DisplayName("执行任务 - 执行失败但未达到最大重试次数")
    void executeTask_FailWithRetry() {
        // Given
        Long taskId = 1L;
        AgentTask task = createMockTask("测试任务", AgentTask.TaskType.KNOWLEDGE_EXTRACTION);
        task.setStatus(AgentTask.TaskStatus.PENDING);
        task.setRetryCount(0);
        task.setMaxRetries(3);
        task.setCronExpression(null);
        task.setId(taskId);

        when(agentTaskRepository.findById(eq(taskId))).thenReturn(Optional.of(task));
        when(agentTaskRepository.save(any(AgentTask.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock ObjectMapper to throw exception during execution
        try {
            when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("JSON序列化失败"));
        } catch (Exception e) {
            // ignore
        }

        // When
        AgentTask result = agentTaskService.executeTask(taskId);

        // Then - 执行失败，但未达到最大重试次数，应该重置为PENDING并增加重试计数
        assertThat(result.getStatus()).isEqualTo(AgentTask.TaskStatus.PENDING);
        assertThat(result.getRetryCount()).isEqualTo(1);
    }

    // ==================== 任务状态管理测试 ====================

    @Test
    @DisplayName("标记任务为运行中")
    void markAsRunning_Success() {
        // Given
        AgentTask task = createMockTaskWithStatus("测试任务", AgentTask.TaskStatus.PENDING);
        when(agentTaskRepository.save(any(AgentTask.class))).thenReturn(task);

        // When
        agentTaskService.markAsRunning(task);

        // Then
        assertThat(task.getStatus()).isEqualTo(AgentTask.TaskStatus.RUNNING);
        assertThat(task.getStartedAt()).isNotNull();

        verify(agentTaskRepository).save(task);
    }

    @Test
    @DisplayName("标记任务为完成")
    void markAsCompleted_Success() {
        // Given
        AgentTask task = createMockTaskWithStatus("测试任务", AgentTask.TaskStatus.RUNNING);
        String result = "{\"status\":\"success\"}";
        when(agentTaskRepository.save(any(AgentTask.class))).thenReturn(task);

        // When
        agentTaskService.markAsCompleted(task, result);

        // Then
        assertThat(task.getStatus()).isEqualTo(AgentTask.TaskStatus.COMPLETED);
        assertThat(task.getCompletedAt()).isNotNull();
        assertThat(task.getResult()).isEqualTo(result);

        verify(agentTaskRepository).save(task);
    }

    @Test
    @DisplayName("标记任务为完成 - 定时任务计算下次执行时间")
    void markAsCompleted_ScheduledTask_CalculatesNextRun() {
        // Given
        AgentTask task = createMockTaskWithStatus("定时任务", AgentTask.TaskStatus.RUNNING);
        task.setCronExpression("0 0 * * *");
        when(agentTaskRepository.save(any(AgentTask.class))).thenReturn(task);

        // When
        agentTaskService.markAsCompleted(task, "{\"status\":\"success\"}");

        // Then
        assertThat(task.getStatus()).isEqualTo(AgentTask.TaskStatus.PENDING); // 定时任务重置为PENDING
        assertThat(task.getNextRunAt()).isNotNull();
        assertThat(task.getCompletedAt()).isNotNull();

        verify(agentTaskRepository).save(task);
    }

    @Test
    @DisplayName("标记任务为失败")
    void markAsFailed_Success() {
        // Given
        AgentTask task = createMockTaskWithStatus("测试任务", AgentTask.TaskStatus.RUNNING);
        String errorMessage = "连接超时";
        when(agentTaskRepository.save(any(AgentTask.class))).thenReturn(task);

        // When
        agentTaskService.markAsFailed(task, errorMessage);

        // Then
        assertThat(task.getStatus()).isEqualTo(AgentTask.TaskStatus.FAILED);
        assertThat(task.getCompletedAt()).isNotNull();
        assertThat(task.getErrorMessage()).isEqualTo(errorMessage);

        verify(agentTaskRepository).save(task);
    }

    @Test
    @DisplayName("标记任务为失败 - 截断过长错误信息")
    void markAsFailed_LongError_Truncates() {
        // Given
        AgentTask task = createMockTaskWithStatus("测试任务", AgentTask.TaskStatus.RUNNING);
        String longError = "a".repeat(2000); // 超过1000字符
        when(agentTaskRepository.save(any(AgentTask.class))).thenReturn(task);

        // When
        agentTaskService.markAsFailed(task, longError);

        // Then
        assertThat(task.getErrorMessage()).hasSize(1000);

        verify(agentTaskRepository).save(task);
    }

    // ==================== 重试任务测试 ====================

    @Test
    @DisplayName("重试失败任务 - 成功")
    void retryTask_Success() {
        // Given
        Long taskId = 1L;
        AgentTask task = createMockTaskWithStatus("测试任务", AgentTask.TaskStatus.FAILED);
        task.setRetryCount(1);
        task.setId(taskId);

        when(agentTaskRepository.findById(eq(taskId))).thenReturn(Optional.of(task));
        when(agentTaskRepository.save(any(AgentTask.class))).thenReturn(task);

        // When
        AgentTask result = agentTaskService.retryTask(taskId);

        // Then
        assertThat(result.getStatus()).isEqualTo(AgentTask.TaskStatus.PENDING);
        assertThat(result.getRetryCount()).isEqualTo(2);
        assertThat(result.getErrorMessage()).isNull();

        verify(agentTaskRepository).save(task);
    }

    @Test
    @DisplayName("重试任务 - 状态不允许")
    void retryTask_InvalidStatus() {
        // Given
        Long taskId = 1L;
        AgentTask task = createMockTaskWithStatus("测试任务", AgentTask.TaskStatus.RUNNING);
        task.setId(taskId);

        when(agentTaskRepository.findById(eq(taskId))).thenReturn(Optional.of(task));

        // When & Then
        assertThatThrownBy(() -> agentTaskService.retryTask(taskId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("只能重试失败的任务");
    }

    // ==================== 取消任务测试 ====================

    @Test
    @DisplayName("取消任务 - 成功")
    void cancelTask_Success() {
        // Given
        Long taskId = 1L;
        AgentTask task = createMockTaskWithStatus("测试任务", AgentTask.TaskStatus.PENDING);
        task.setId(taskId);
        task.setUserId(TEST_USER_ID);

        when(agentTaskRepository.findById(eq(taskId))).thenReturn(Optional.of(task));
        when(agentTaskRepository.save(any(AgentTask.class))).thenReturn(task);

        // When
        agentTaskService.cancelTask(taskId, TEST_USER_ID);

        // Then
        assertThat(task.getStatus()).isEqualTo(AgentTask.TaskStatus.CANCELLED);

        verify(agentTaskRepository).save(task);
    }

    @Test
    @DisplayName("取消任务 - 无权限")
    void cancelTask_NoPermission() {
        // Given
        Long taskId = 1L;
        AgentTask task = createMockTaskWithStatus("测试任务", AgentTask.TaskStatus.PENDING);
        task.setId(taskId);
        task.setUserId("other-user");

        when(agentTaskRepository.findById(eq(taskId))).thenReturn(Optional.of(task));

        // When & Then
        assertThatThrownBy(() -> agentTaskService.cancelTask(taskId, TEST_USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无权");
    }

    @Test
    @DisplayName("取消任务 - 无法取消正在执行的任务")
    void cancelTask_Running_Failed() {
        // Given
        Long taskId = 1L;
        AgentTask task = createMockTaskWithStatus("测试任务", AgentTask.TaskStatus.RUNNING);
        task.setId(taskId);
        task.setUserId(TEST_USER_ID);

        when(agentTaskRepository.findById(eq(taskId))).thenReturn(Optional.of(task));

        // When & Then
        assertThatThrownBy(() -> agentTaskService.cancelTask(taskId, TEST_USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("无法取消正在执行");
    }

    // ==================== 超时任务处理测试 ====================

    @Test
    @DisplayName("处理超时任务 - 重试")
    void handleTimeoutTask_WithRetry() {
        // Given
        AgentTask task = createMockTaskWithStatus("测试任务", AgentTask.TaskStatus.RUNNING);
        task.setRetryCount(0);
        task.setMaxRetries(3);
        when(agentTaskRepository.save(any(AgentTask.class))).thenReturn(task);

        // When
        agentTaskService.handleTimeoutTask(task);

        // Then
        assertThat(task.getStatus()).isEqualTo(AgentTask.TaskStatus.PENDING);
        assertThat(task.getRetryCount()).isEqualTo(1);

        verify(agentTaskRepository).save(task);
    }

    @Test
    @DisplayName("处理超时任务 - 达到最大重试次数")
    void handleTimeoutTask_MaxRetries() {
        // Given
        AgentTask task = createMockTaskWithStatus("测试任务", AgentTask.TaskStatus.RUNNING);
        task.setRetryCount(3);
        task.setMaxRetries(3);
        when(agentTaskRepository.save(any(AgentTask.class))).thenReturn(task);

        // When
        agentTaskService.handleTimeoutTask(task);

        // Then
        assertThat(task.getStatus()).isEqualTo(AgentTask.TaskStatus.FAILED);
        assertThat(task.getErrorMessage()).contains("超时");

        verify(agentTaskRepository).save(task);
    }

    // ==================== 查询任务测试 ====================

    @Test
    @DisplayName("获取待执行任务列表")
    void getPendingTasks_Success() {
        // Given
        List<AgentTask> pendingTasks = Arrays.asList(
                createMockTask("任务1", AgentTask.TaskType.KNOWLEDGE_EXTRACTION),
                createMockTask("任务2", AgentTask.TaskType.CONTENT_GENERATION)
        );

        when(agentTaskRepository.findPendingTasks(any(LocalDateTime.class)))
                .thenReturn(pendingTasks);

        // When
        List<AgentTask> result = agentTaskService.getPendingTasks();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(t -> t.getStatus() == AgentTask.TaskStatus.PENDING);

        verify(agentTaskRepository).findPendingTasks(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("获取超时任务列表")
    void getTimeoutTasks_Success() {
        // Given
        LocalDateTime timeout = LocalDateTime.now().minusMinutes(30);
        List<AgentTask> timeoutTasks = Collections.singletonList(
                createMockTaskWithStatus("超时任务", AgentTask.TaskStatus.RUNNING)
        );

        when(agentTaskRepository.findTimeoutTasks(eq(timeout)))
                .thenReturn(timeoutTasks);

        // When
        List<AgentTask> result = agentTaskService.getTimeoutTasks(timeout);

        // Then
        assertThat(result).hasSize(1);

        verify(agentTaskRepository).findTimeoutTasks(timeout);
    }

    @Test
    @DisplayName("获取可重试任务列表")
    void getRetryableTasks_Success() {
        // Given
        List<AgentTask> retryableTasks = Arrays.asList(
                createMockTaskWithStatus("重试任务1", AgentTask.TaskStatus.FAILED),
                createMockTaskWithStatus("重试任务2", AgentTask.TaskStatus.FAILED)
        );

        when(agentTaskRepository.findRetryableTasks()).thenReturn(retryableTasks);

        // When
        List<AgentTask> result = agentTaskService.getRetryableTasks();

        // Then
        assertThat(result).hasSize(2);

        verify(agentTaskRepository).findRetryableTasks();
    }

    @Test
    @DisplayName("统计任务执行情况")
    void countTasksByStatus_Success() {
        // Given
        AgentTask.TaskStatus status = AgentTask.TaskStatus.COMPLETED;
        long expectedCount = 10L;

        when(agentTaskRepository.countByUserIdAndStatus(eq(TEST_USER_ID), eq(status)))
                .thenReturn(expectedCount);

        // When
        long result = agentTaskService.countTasksByStatus(TEST_USER_ID, status);

        // Then
        assertThat(result).isEqualTo(expectedCount);

        verify(agentTaskRepository).countByUserIdAndStatus(TEST_USER_ID, status);
    }

    // ==================== 删除任务测试 ====================

    @Test
    @DisplayName("删除任务 - 成功")
    void deleteTask_Success() {
        // Given
        Long taskId = 1L;
        AgentTask task = createMockTaskWithStatus("测试任务", AgentTask.TaskStatus.COMPLETED);
        task.setId(taskId);
        task.setUserId(TEST_USER_ID);

        when(agentTaskRepository.findById(eq(taskId))).thenReturn(Optional.of(task));
        doNothing().when(agentTaskRepository).delete(any(AgentTask.class));

        // When
        agentTaskService.deleteTask(taskId, TEST_USER_ID);

        // Then
        verify(agentTaskRepository).findById(taskId);
        verify(agentTaskRepository).delete(task);
    }

    @Test
    @DisplayName("删除任务 - 无权限")
    void deleteTask_NoPermission() {
        // Given
        Long taskId = 1L;
        AgentTask task = createMockTaskWithStatus("测试任务", AgentTask.TaskStatus.COMPLETED);
        task.setId(taskId);
        task.setUserId("other-user");

        when(agentTaskRepository.findById(eq(taskId))).thenReturn(Optional.of(task));

        // When & Then
        assertThatThrownBy(() -> agentTaskService.deleteTask(taskId, TEST_USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无权");

        verify(agentTaskRepository).findById(taskId);
        verify(agentTaskRepository, never()).delete(any());
    }

    // ==================== 辅助方法 ====================

    private AgentTask createMockTask(String taskName, AgentTask.TaskType taskType) {
        return AgentTask.builder()
                .id(1L)
                .taskName(taskName)
                .taskType(taskType)
                .userId(TEST_USER_ID)
                .status(AgentTask.TaskStatus.PENDING)
                .priority(2)
                .retryCount(0)
                .maxRetries(3)
                .build();
    }

    private AgentTask createMockTaskWithStatus(String taskName, AgentTask.TaskStatus status) {
        return AgentTask.builder()
                .id(1L)
                .taskName(taskName)
                .taskType(AgentTask.TaskType.KNOWLEDGE_EXTRACTION)
                .userId(TEST_USER_ID)
                .status(status)
                .priority(2)
                .retryCount(0)
                .maxRetries(3)
                .build();
    }
}
