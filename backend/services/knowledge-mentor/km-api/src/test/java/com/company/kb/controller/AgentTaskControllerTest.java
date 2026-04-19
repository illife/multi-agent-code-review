package com.company.kb.controller;

import com.company.kb.core.domain.AgentTask;
import com.company.kb.core.service.AgentTaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AgentTaskController 集成测试
 * 测试智能体任务API接口
 */
@WebMvcTest(AgentTaskController.class)
@DisplayName("智能体任务控制器集成测试")
class AgentTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AgentTaskService agentTaskService;

    private static final String TEST_USER = "test-user";

    @BeforeEach
    void setUp() {
        reset(agentTaskService);
    }

    // ==================== 创建任务测试 ====================

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("POST /agent-tasks/create - 创建新任务成功")
    void createTask_Success() throws Exception {
        // Given
        AgentTaskController.CreateTaskRequest request = new AgentTaskController.CreateTaskRequest();
        request.setTaskName("知识提取任务");
        request.setTaskType(AgentTask.TaskType.KNOWLEDGE_EXTRACTION);
        request.setConfig("{\"source\":\"document\"}");
        request.setPriority(1);

        AgentTask mockTask = AgentTask.builder()
                .id(1L)
                .taskName("知识提取任务")
                .taskType(AgentTask.TaskType.KNOWLEDGE_EXTRACTION)
                .userId(TEST_USER)
                .status(AgentTask.TaskStatus.PENDING)
                .priority(1)
                .build();

        when(agentTaskService.createTask(
                eq("知识提取任务"),
                eq(AgentTask.TaskType.KNOWLEDGE_EXTRACTION),
                eq(TEST_USER),
                eq("{\"source\":\"document\"}"),
                eq(1),
                eq(TEST_USER)
        )).thenReturn(mockTask);

        // When & Then
        mockMvc.perform(post("/agent-tasks/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.taskName").value("知识提取任务"))
                .andExpect(jsonPath("$.data.taskType").value("KNOWLEDGE_EXTRACTION"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.priority").value(1));
    }

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("POST /agent-tasks/scheduled - 创建定时任务成功")
    void createScheduledTask_Success() throws Exception {
        // Given
        AgentTaskController.ScheduledTaskRequest request = new AgentTaskController.ScheduledTaskRequest();
        request.setTaskName("定时内容生成");
        request.setTaskType(AgentTask.TaskType.CONTENT_GENERATION);
        request.setConfig("{\"template\":\"lesson\"}");
        request.setCronExpression("0 0 * * *");

        AgentTask mockTask = AgentTask.builder()
                .id(1L)
                .taskName("定时内容生成")
                .taskType(AgentTask.TaskType.CONTENT_GENERATION)
                .userId(TEST_USER)
                .status(AgentTask.TaskStatus.PENDING)
                .cronExpression("0 0 * * *")
                .nextRunAt(LocalDateTime.now().plusMinutes(1))
                .build();

        when(agentTaskService.createScheduledTask(
                anyString(),
                any(AgentTask.TaskType.class),
                eq(TEST_USER),
                anyString(),
                eq("0 0 * * *"),
                eq(TEST_USER)
        )).thenReturn(mockTask);

        // When & Then
        mockMvc.perform(post("/agent-tasks/scheduled")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cronExpression").value("0 0 * * *"))
                .andExpect(jsonPath("$.data.nextRunAt").isNotEmpty());
    }

    // ==================== 执行任务测试 ====================

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("POST /agent-tasks/{id}/execute - 手动执行任务成功")
    void executeTask_Success() throws Exception {
        // Given
        Long taskId = 1L;
        AgentTask executedTask = AgentTask.builder()
                .id(taskId)
                .taskName("测试任务")
                .status(AgentTask.TaskStatus.COMPLETED)
                .build();

        when(agentTaskService.executeTask(eq(taskId))).thenReturn(executedTask);

        // When & Then
        mockMvc.perform(post("/agent-tasks/{taskId}/execute", taskId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("任务已提交执行"));

        verify(agentTaskService).executeTask(taskId);
    }

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("POST /agent-tasks/{id}/execute - 任务不存在")
    void executeTask_NotFound() throws Exception {
        // Given
        Long taskId = 999L;
        when(agentTaskService.executeTask(eq(taskId)))
                .thenThrow(new IllegalArgumentException("任务不存在: 999"));

        // When & Then
        mockMvc.perform(post("/agent-tasks/{taskId}/execute", taskId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("任务不存在: 999"));
    }

    // ==================== 重试任务测试 ====================

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("POST /agent-tasks/{id}/retry - 重试失败任务成功")
    void retryTask_Success() throws Exception {
        // Given
        Long taskId = 1L;
        AgentTask retryTask = AgentTask.builder()
                .id(taskId)
                .taskName("重试任务")
                .status(AgentTask.TaskStatus.PENDING)
                .retryCount(1)
                .build();

        when(agentTaskService.retryTask(eq(taskId))).thenReturn(retryTask);

        // When & Then
        mockMvc.perform(post("/agent-tasks/{taskId}/retry", taskId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.retryCount").value(1));
    }

    // ==================== 取消任务测试 ====================

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("POST /agent-tasks/{id}/cancel - 取消任务成功")
    void cancelTask_Success() throws Exception {
        // Given
        Long taskId = 1L;
        doNothing().when(agentTaskService).cancelTask(eq(taskId), eq(TEST_USER));

        // When & Then
        mockMvc.perform(post("/agent-tasks/{taskId}/cancel", taskId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("任务已取消"));

        verify(agentTaskService).cancelTask(taskId, TEST_USER);
    }

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("POST /agent-tasks/{id}/cancel - 无权限取消")
    void cancelTask_NoPermission() throws Exception {
        // Given
        Long taskId = 1L;
        doThrow(new IllegalArgumentException("无权取消此任务"))
                .when(agentTaskService).cancelTask(eq(taskId), eq(TEST_USER));

        // When & Then
        mockMvc.perform(post("/agent-tasks/{taskId}/cancel", taskId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("无权取消此任务"));
    }

    // ==================== 查询任务测试 ====================

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("GET /agent-tasks - 获取用户任务列表（分页）")
    void getUserTasks_Success() throws Exception {
        // Given
        List<AgentTask> tasks = Arrays.asList(
                        AgentTask.builder().id(1L).taskName("任务1").status(AgentTask.TaskStatus.PENDING).build(),
                        AgentTask.builder().id(2L).taskName("任务2").status(AgentTask.TaskStatus.COMPLETED).build()
                );

        Page<AgentTask> page = new PageImpl<>(tasks, PageRequest.of(0, 10), 2);

        when(agentTaskService.getUserTasks(eq(TEST_USER), any()))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/agent-tasks")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.data.length()").value(2))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.currentPage").value(0));
    }

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("GET /agent-tasks/{id} - 获取任务详情")
    void getTask_Success() throws Exception {
        // Given
        Long taskId = 1L;
        AgentTask task = AgentTask.builder()
                .id(taskId)
                .taskName("测试任务")
                .taskType(AgentTask.TaskType.KNOWLEDGE_EXTRACTION)
                .status(AgentTask.TaskStatus.COMPLETED)
                .result("{\"status\":\"success\"}")
                .build();

        when(agentTaskService.getTaskById(eq(taskId))).thenReturn(task);

        // When & Then
        mockMvc.perform(get("/agent-tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.taskName").value("测试任务"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.result").isNotEmpty());
    }

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("GET /agent-tasks/search - 搜索任务")
    void searchTasks_Success() throws Exception {
        // Given
        String keyword = "知识";
        List<AgentTask> tasks = Arrays.asList(
                AgentTask.builder().id(1L).taskName("知识提取任务").build(),
                AgentTask.builder().id(2L).taskName("知识生成任务").build()
        );

        Page<AgentTask> page = new PageImpl<>(tasks, PageRequest.of(0, 10), 2);

        when(agentTaskService.searchTasks(eq(TEST_USER), eq(keyword), any()))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/agent-tasks/search")
                        .param("keyword", keyword)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.data.length()").value(2));
    }

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("GET /agent-tasks/stats - 获取任务统计")
    void getStats_Success() throws Exception {
        // Given
        when(agentTaskService.countTasksByStatus(eq(TEST_USER), eq(AgentTask.TaskStatus.PENDING)))
                .thenReturn(5L);
        when(agentTaskService.countTasksByStatus(eq(TEST_USER), eq(AgentTask.TaskStatus.RUNNING)))
                .thenReturn(2L);
        when(agentTaskService.countTasksByStatus(eq(TEST_USER), eq(AgentTask.TaskStatus.COMPLETED)))
                .thenReturn(20L);
        when(agentTaskService.countTasksByStatus(eq(TEST_USER), eq(AgentTask.TaskStatus.FAILED)))
                .thenReturn(3L);
        when(agentTaskService.countTasksByStatus(eq(TEST_USER), eq(AgentTask.TaskStatus.CANCELLED)))
                .thenReturn(1L);

        // When & Then
        mockMvc.perform(get("/agent-tasks/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.pending").value(5))
                .andExpect(jsonPath("$.data.running").value(2))
                .andExpect(jsonPath("$.data.completed").value(20))
                .andExpect(jsonPath("$.data.failed").value(3))
                .andExpect(jsonPath("$.data.cancelled").value(1));
    }

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("GET /agent-tasks/pending - 获取待执行任务（管理员）")
    void getPendingTasks_Success() throws Exception {
        // Given
        List<AgentTask> pendingTasks = Arrays.asList(
                AgentTask.builder().id(1L).taskName("待执行任务1").status(AgentTask.TaskStatus.PENDING).build(),
                AgentTask.builder().id(2L).taskName("待执行任务2").status(AgentTask.TaskStatus.PENDING).build()
        );

        when(agentTaskService.getPendingTasks()).thenReturn(pendingTasks);

        // When & Then
        mockMvc.perform(get("/agent-tasks/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    // ==================== 删除任务测试 ====================

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("DELETE /agent-tasks/{id} - 删除任务成功")
    void deleteTask_Success() throws Exception {
        // Given
        Long taskId = 1L;
        doNothing().when(agentTaskService).deleteTask(eq(taskId), eq(TEST_USER));

        // When & Then
        mockMvc.perform(delete("/agent-tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("任务删除成功"));

        verify(agentTaskService).deleteTask(taskId, TEST_USER);
    }

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("DELETE /agent-tasks/{id} - 任务不存在")
    void deleteTask_NotFound() throws Exception {
        // Given
        Long taskId = 999L;
        doThrow(new IllegalArgumentException("任务不存在: 999"))
                .when(agentTaskService).deleteTask(eq(taskId), eq(TEST_USER));

        // When & Then
        mockMvc.perform(delete("/agent-tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("任务不存在: 999"));
    }

    // ==================== 参数验证测试 ====================

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("POST /agent-tasks/create - 缺少必需参数")
    void createTask_MissingRequiredParam_Failed() throws Exception {
        // Given - 缺少taskName
        AgentTaskController.CreateTaskRequest request = new AgentTaskController.CreateTaskRequest();
        request.setTaskType(AgentTask.TaskType.KNOWLEDGE_EXTRACTION);
        // taskName 未设置

        // When & Then
        mockMvc.perform(post("/agent-tasks/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError()); // 可能导致NPE
    }

    @Test
    @DisplayName("未认证用户访问 - 返回401或403")
    void unauthenticatedUser_AccessDenied() throws Exception {
        mockMvc.perform(get("/agent-tasks"))
                .andExpect(status().is4xxClientError());
    }
}
