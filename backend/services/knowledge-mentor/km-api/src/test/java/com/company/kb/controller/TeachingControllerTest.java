package com.company.kb.controller;

import com.company.kb.core.domain.TeachingDocument;
import com.company.kb.core.service.TeachingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TeachingController 集成测试
 * 测试教学文档API接口
 */
@WebMvcTest(TeachingController.class)
@DisplayName("教学文档控制器集成测试")
class TeachingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TeachingService teachingService;

    private static final String TEST_USER = "test-user";

    @BeforeEach
    void setUp() {
        reset(teachingService);
    }

    // ==================== 生成教学文档测试 ====================

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("POST /teaching/generate - 生成课时教学文档")
    void generateTeachingDocument_Lesson_Success() throws Exception {
        // Given
        TeachingController.GenerateTeachingRequest request = new TeachingController.GenerateTeachingRequest();
        request.setDocumentType(TeachingDocument.DocumentType.LESSON);
        request.setTitle("Java基础教学");
        request.setKnowledgePointIds(Arrays.asList("1", "2"));

        TeachingDocument mockDocument = TeachingDocument.builder()
                .id(1L)
                .title("Java基础教学")
                .documentType(TeachingDocument.DocumentType.LESSON)
                .status(TeachingDocument.DocumentStatus.PUBLISHED)
                .content("# Java基础教学\n\n## 学习目标\n...")
                .build();

        when(teachingService.generateTeachingDocument(
                eq(TEST_USER),
                eq(TeachingDocument.DocumentType.LESSON),
                isNull(),
                anyList(),
                eq("Java基础教学"),
                eq(TEST_USER)
        )).thenReturn(mockDocument);

        // When & Then
        mockMvc.perform(post("/teaching/generate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Java基础教学"))
                .andExpect(jsonPath("$.data.documentType").value("LESSON"))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("POST /teaching/generate - 生成练习文档")
    void generateTeachingDocument_Practice_Success() throws Exception {
        // Given
        TeachingController.GenerateTeachingRequest request = new TeachingController.GenerateTeachingRequest();
        request.setDocumentType(TeachingDocument.DocumentType.PRACTICE);
        request.setTitle("练习题集");
        request.setKnowledgePointIds(Collections.singletonList("1"));

        TeachingDocument mockDocument = TeachingDocument.builder()
                .id(2L)
                .title("练习题集")
                .documentType(TeachingDocument.DocumentType.PRACTICE)
                .status(TeachingDocument.DocumentStatus.PUBLISHED)
                .content("## 练习题\n...")
                .build();

        when(teachingService.generateTeachingDocument(
                anyString(),
                eq(TeachingDocument.DocumentType.PRACTICE),
                isNull(),
                anyList(),
                anyString(),
                anyString()
        )).thenReturn(mockDocument);

        // When & Then
        mockMvc.perform(post("/teaching/generate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentType").value("PRACTICE"));
    }

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("POST /teaching/personalized - 生成个性化教学文档")
    void generatePersonalizedLesson_Success() throws Exception {
        // Given
        TeachingController.PersonalizedLessonRequest request = new TeachingController.PersonalizedLessonRequest();
        request.setTestResultId(100L);
        request.setWeakPoints(Arrays.asList("Java集合框架", "多线程"));

        TeachingDocument mockDocument = TeachingDocument.builder()
                .id(3L)
                .title("个性化教学 - " + java.time.LocalDate.now())
                .documentType(TeachingDocument.DocumentType.LESSON)
                .testResultId(100L)
                .priority(1)
                .status(TeachingDocument.DocumentStatus.PUBLISHED)
                .build();

        when(teachingService.generatePersonalizedLesson(
                eq(TEST_USER),
                eq(100L),
                anyList(),
                eq(TEST_USER)
        )).thenReturn(mockDocument);

        // When & Then
        mockMvc.perform(post("/teaching/personalized")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.testResultId").value(100))
                .andExpect(jsonPath("$.data.priority").value(1));
    }

    // ==================== 查询教学文档测试 ====================

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("GET /teaching/documents - 获取用户文档列表")
    void getUserDocuments_Success() throws Exception {
        // Given
        List<TeachingDocument> documents = Arrays.asList(
                TeachingDocument.builder().id(1L).title("文档1").documentType(TeachingDocument.DocumentType.LESSON).build(),
                TeachingDocument.builder().id(2L).title("文档2").documentType(TeachingDocument.DocumentType.PRACTICE).build()
        );

        when(teachingService.getUserTeachingDocuments(eq(TEST_USER))).thenReturn(documents);

        // When & Then
        mockMvc.perform(get("/teaching/documents")
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
    @DisplayName("GET /teaching/published - 获取已发布文档")
    void getPublishedDocuments_Success() throws Exception {
        // Given
        List<TeachingDocument> documents = Arrays.asList(
                TeachingDocument.builder().id(1L).title("已发布文档1").status(TeachingDocument.DocumentStatus.PUBLISHED).build(),
                TeachingDocument.builder().id(2L).title("已发布文档2").status(TeachingDocument.DocumentStatus.PUBLISHED).build()
        );

        when(teachingService.getPublishedTeachingDocuments(eq(TEST_USER))).thenReturn(documents);

        // When & Then
        mockMvc.perform(get("/teaching/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("GET /teaching/search - 搜索文档")
    void searchDocuments_Success() throws Exception {
        // Given
        String keyword = "Java";
        List<TeachingDocument> documents = Arrays.asList(
                TeachingDocument.builder().id(1L).title("Java基础教程").build(),
                TeachingDocument.builder().id(2L).title("Java高级编程").build()
        );

        when(teachingService.searchTeachingDocuments(eq(TEST_USER), eq(keyword))).thenReturn(documents);

        // When & Then
        mockMvc.perform(get("/teaching/search")
                        .param("keyword", keyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("GET /teaching/stats - 获取统计信息")
    void getStats_Success() throws Exception {
        // Given
        List<TeachingDocument> allDocuments = Arrays.asList(
                TeachingDocument.builder().status(TeachingDocument.DocumentStatus.PUBLISHED).documentType(TeachingDocument.DocumentType.LESSON).build(),
                TeachingDocument.builder().status(TeachingDocument.DocumentStatus.DRAFT).documentType(TeachingDocument.DocumentType.PRACTICE).build(),
                TeachingDocument.builder().status(TeachingDocument.DocumentStatus.PUBLISHED).documentType(TeachingDocument.DocumentType.REVIEW).build()
        );

        when(teachingService.getUserTeachingDocuments(eq(TEST_USER))).thenReturn(allDocuments);

        // When & Then
        mockMvc.perform(get("/teaching/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.published").value(2))
                .andExpect(jsonPath("$.data.draft").value(1))
                .andExpect(jsonPath("$.data.byType.LESSON").value(1));
    }

    // ==================== 删除教学文档测试 ====================

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("DELETE /teaching/documents/{id} - 删除文档成功")
    void deleteDocument_Success() throws Exception {
        // Given
        Long documentId = 1L;
        doNothing().when(teachingService).deleteTeachingDocument(eq(documentId), eq(TEST_USER));

        // When & Then
        mockMvc.perform(delete("/teaching/documents/{documentId}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("教学文档删除成功"));

        verify(teachingService).deleteTeachingDocument(documentId, TEST_USER);
    }

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("DELETE /teaching/documents/{id} - 删除文档失败（无权限）")
    void deleteDocument_NoPermission() throws Exception {
        // Given
        Long documentId = 1L;
        doThrow(new IllegalArgumentException("无权删除此文档"))
                .when(teachingService).deleteTeachingDocument(eq(documentId), eq(TEST_USER));

        // When & Then
        mockMvc.perform(delete("/teaching/documents/{documentId}", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("无权删除此文档"));
    }

    // ==================== 参数验证测试 ====================

    @Test
    @WithMockUser(username = TEST_USER)
    @DisplayName("POST /teaching/generate - 缺少必需参数")
    void generateTeachingDocument_MissingParam_Failed() throws Exception {
        // Given - 缺少title
        TeachingController.GenerateTeachingRequest request = new TeachingController.GenerateTeachingRequest();
        request.setDocumentType(TeachingDocument.DocumentType.LESSON);
        // title 未设置

        // When & Then
        mockMvc.perform(post("/teaching/generate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError()); // 可能导致NPE
    }

    @Test
    @DisplayName("未认证用户访问 - 返回401或403")
    void unauthenticatedUser_AccessDenied() throws Exception {
        mockMvc.perform(get("/teaching/documents"))
                .andExpect(status().is4xxClientError());
    }
}
