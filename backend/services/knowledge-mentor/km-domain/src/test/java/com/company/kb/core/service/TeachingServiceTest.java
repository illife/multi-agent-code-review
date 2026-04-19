package com.company.kb.core.service;

import com.company.kb.core.domain.Document;
import com.company.kb.core.domain.DocumentChunk;
import com.company.kb.core.domain.TeachingDocument;
import com.company.kb.core.repository.DocumentChunkRepository;
import com.company.kb.core.repository.DocumentRepository;
import com.company.kb.core.repository.TeachingDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TeachingService 单元测试
 * 测试教学文档生成功能的各个场景
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("教学服务测试")
class TeachingServiceTest {

    @Mock
    private TeachingDocumentRepository teachingDocumentRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @InjectMocks
    private TeachingService teachingService;

    private static final String TEST_USER_ID = "test-user";
    private static final String TEST_CREATOR = "system";

    @BeforeEach
    void setUp() {
        // 重置mock对象
        reset(teachingDocumentRepository, documentRepository, documentChunkRepository);
    }

    @Test
    @DisplayName("生成课时教学文档 - 成功")
    void generateTeachingDocument_Lesson_Success() {
        // Given
        List<String> knowledgePointIds = Arrays.asList("1", "2", "3");
        String title = "Java基础教学";
        List<DocumentChunk> chunks = createMockChunks();

        when(documentChunkRepository.findByDocumentId(anyLong()))
                .thenReturn(chunks);
        when(teachingDocumentRepository.save(any(TeachingDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TeachingDocument result = teachingService.generateTeachingDocument(
                TEST_USER_ID,
                TeachingDocument.DocumentType.LESSON,
                null,
                knowledgePointIds,
                title,
                TEST_CREATOR
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo(title);
        assertThat(result.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(result.getDocumentType()).isEqualTo(TeachingDocument.DocumentType.LESSON);
        assertThat(result.getStatus()).isEqualTo(TeachingDocument.DocumentStatus.PUBLISHED);
        assertThat(result.getContent()).contains("# " + title);
        assertThat(result.getContent()).contains("## 学习目标");
        assertThat(result.getContent()).contains("## 知识要点");
        assertThat(result.getContent()).contains("## 思考题");

        verify(documentChunkRepository, times(3)).findByDocumentId(anyLong());
        verify(teachingDocumentRepository).save(any(TeachingDocument.class));
    }

    @Test
    @DisplayName("生成练习教学文档 - 成功")
    void generateTeachingDocument_Practice_Success() {
        // Given
        List<String> knowledgePointIds = Collections.singletonList("1");
        String title = "练习题集";
        List<DocumentChunk> chunks = createMockChunks();

        when(documentChunkRepository.findByDocumentId(anyLong()))
                .thenReturn(chunks);
        when(teachingDocumentRepository.save(any(TeachingDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TeachingDocument result = teachingService.generateTeachingDocument(
                TEST_USER_ID,
                TeachingDocument.DocumentType.PRACTICE,
                null,
                knowledgePointIds,
                title,
                TEST_CREATOR
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDocumentType()).isEqualTo(TeachingDocument.DocumentType.PRACTICE);
        assertThat(result.getContent()).contains("## 练习题");
        assertThat(result.getContent()).contains("### 练习 1");
        assertThat(result.getContent()).contains("## 解析与答案");
    }

    @Test
    @DisplayName("生成复习教学文档 - 成功")
    void generateTeachingDocument_Review_Success() {
        // Given
        List<String> knowledgePointIds = Collections.singletonList("1");
        String title = "期中复习";
        List<DocumentChunk> chunks = createMockChunks();

        when(documentChunkRepository.findByDocumentId(anyLong()))
                .thenReturn(chunks);
        when(teachingDocumentRepository.save(any(TeachingDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TeachingDocument result = teachingService.generateTeachingDocument(
                TEST_USER_ID,
                TeachingDocument.DocumentType.REVIEW,
                null,
                knowledgePointIds,
                title,
                TEST_CREATOR
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDocumentType()).isEqualTo(TeachingDocument.DocumentType.REVIEW);
        assertThat(result.getContent()).contains("## 复习要点");
        assertThat(result.getContent()).contains("## 重点记忆");
        assertThat(result.getContent()).contains("## 常见错误");
    }

    @Test
    @DisplayName("生成知识缺口分析文档 - 成功")
    void generateTeachingDocument_KnowledgeGap_Success() {
        // Given
        List<String> knowledgePointIds = Collections.emptyList();
        String title = "知识缺口分析";

        when(teachingDocumentRepository.save(any(TeachingDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TeachingDocument result = teachingService.generateTeachingDocument(
                TEST_USER_ID,
                TeachingDocument.DocumentType.KNOWLEDGE_GAP,
                null,
                knowledgePointIds,
                title,
                TEST_CREATOR
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDocumentType()).isEqualTo(TeachingDocument.DocumentType.KNOWLEDGE_GAP);
        assertThat(result.getContent()).contains("## 知识缺口分析");
        assertThat(result.getContent()).contains("### 当前水平评估");
        assertThat(result.getContent()).contains("### 改进建议");
        assertThat(result.getContent()).contains("### 推荐学习资源");
    }

    @Test
    @DisplayName("生成个性化教学文档 - 薄弱知识点补习")
    void generatePersonalizedLesson_WeakPoints_Success() {
        // Given
        List<String> weakPoints = Arrays.asList("1", "2", "3"); // 使用数字ID
        Long testResultId = 100L;
        List<DocumentChunk> chunks = createMockChunks();

        when(documentChunkRepository.findByDocumentId(anyLong()))
                .thenReturn(chunks);
        when(teachingDocumentRepository.save(any(TeachingDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TeachingDocument result = teachingService.generatePersonalizedLesson(
                TEST_USER_ID,
                testResultId,
                weakPoints,
                TEST_CREATOR
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDocumentType()).isEqualTo(TeachingDocument.DocumentType.LESSON);
        assertThat(result.getTestResultId()).isEqualTo(testResultId);
        assertThat(result.getPriority()).isEqualTo(1); // 高优先级
        assertThat(result.getContent()).contains("# 个性化学习计划");
        assertThat(result.getContent()).contains("## 薄弱知识点分析");
        assertThat(result.getContent()).contains("## 推荐学习内容");

        verify(documentChunkRepository, times(3)).findByDocumentId(anyLong());
        verify(teachingDocumentRepository).save(any(TeachingDocument.class));
    }

    @Test
    @DisplayName("生成教学文档 - 空知识点列表")
    void generateTeachingDocument_EmptyKnowledgePoints() {
        // Given
        List<String> knowledgePointIds = Collections.emptyList();
        String title = "空知识点测试";

        when(teachingDocumentRepository.save(any(TeachingDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        TeachingDocument result = teachingService.generateTeachingDocument(
                TEST_USER_ID,
                TeachingDocument.DocumentType.LESSON,
                null,
                knowledgePointIds,
                title,
                TEST_CREATOR
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).contains("# " + title);
        verify(documentChunkRepository, never()).findByDocumentId(anyLong());
    }

    @Test
    @DisplayName("获取用户教学文档列表")
    void getUserTeachingDocuments_Success() {
        // Given
        List<TeachingDocument> documents = Arrays.asList(
                createMockDocument(TeachingDocument.DocumentType.LESSON),
                createMockDocument(TeachingDocument.DocumentType.PRACTICE)
        );

        when(teachingDocumentRepository.findByUserIdOrderByCreatedAtDesc(eq(TEST_USER_ID)))
                .thenReturn(documents);

        // When
        List<TeachingDocument> result = teachingService.getUserTeachingDocuments(TEST_USER_ID);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDocumentType()).isEqualTo(TeachingDocument.DocumentType.LESSON);
        assertThat(result.get(1).getDocumentType()).isEqualTo(TeachingDocument.DocumentType.PRACTICE);
        verify(teachingDocumentRepository).findByUserIdOrderByCreatedAtDesc(TEST_USER_ID);
    }

    @Test
    @DisplayName("获取已发布的教学文档")
    void getPublishedTeachingDocuments_Success() {
        // Given
        List<TeachingDocument> documents = Arrays.asList(
                createMockDocument(TeachingDocument.DocumentType.LESSON, TeachingDocument.DocumentStatus.PUBLISHED),
                createMockDocument(TeachingDocument.DocumentType.PRACTICE, TeachingDocument.DocumentStatus.PUBLISHED)
        );

        when(teachingDocumentRepository.findPublishedByUserId(eq(TEST_USER_ID)))
                .thenReturn(documents);

        // When
        List<TeachingDocument> result = teachingService.getPublishedTeachingDocuments(TEST_USER_ID);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(d -> d.getStatus() == TeachingDocument.DocumentStatus.PUBLISHED);
        verify(teachingDocumentRepository).findPublishedByUserId(TEST_USER_ID);
    }

    @Test
    @DisplayName("搜索教学文档 - 关键词匹配")
    void searchTeachingDocuments_KeywordMatch_Success() {
        // Given
        String keyword = "Java";
        List<TeachingDocument> documents = Arrays.asList(
                createMockDocument("Java基础教程", TeachingDocument.DocumentType.LESSON),
                createMockDocument("Python入门", TeachingDocument.DocumentType.PRACTICE),
                createMockDocument("Java高级编程", TeachingDocument.DocumentType.REVIEW)
        );

        when(teachingDocumentRepository.searchByKeyword(eq(TEST_USER_ID), eq(keyword)))
                .thenReturn(documents);

        // When
        List<TeachingDocument> result = teachingService.searchTeachingDocuments(TEST_USER_ID, keyword);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).anyMatch(d -> d.getTitle().contains("Java"));
        verify(teachingDocumentRepository).searchByKeyword(TEST_USER_ID, keyword);
    }

    @Test
    @DisplayName("删除教学文档 - 成功")
    void deleteTeachingDocument_Success() {
        // Given
        Long documentId = 1L;
        TeachingDocument document = createMockDocument(documentId, TEST_USER_ID);

        when(teachingDocumentRepository.findById(eq(documentId)))
                .thenReturn(java.util.Optional.of(document));
        doNothing().when(teachingDocumentRepository).delete(any(TeachingDocument.class));

        // When
        teachingService.deleteTeachingDocument(documentId, TEST_USER_ID);

        // Then
        verify(teachingDocumentRepository).findById(documentId);
        verify(teachingDocumentRepository).delete(document);
    }

    @Test
    @DisplayName("删除教学文档 - 无权限")
    void deleteTeachingDocument_NoPermission_Failed() {
        // Given
        Long documentId = 1L;
        TeachingDocument document = createMockDocument(documentId, "other-user");

        when(teachingDocumentRepository.findById(eq(documentId)))
                .thenReturn(java.util.Optional.of(document));

        // When & Then
        assertThatThrownBy(() -> teachingService.deleteTeachingDocument(documentId, TEST_USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无权删除");

        verify(teachingDocumentRepository).findById(documentId);
        verify(teachingDocumentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("删除教学文档 - 文档不存在")
    void deleteTeachingDocument_NotFound_Failed() {
        // Given
        Long documentId = 999L;

        when(teachingDocumentRepository.findById(eq(documentId)))
                .thenReturn(java.util.Optional.empty());

        // When & Then
        assertThatThrownBy(() -> teachingService.deleteTeachingDocument(documentId, TEST_USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不存在");
    }

    // ==================== 辅助方法 ====================

    private List<DocumentChunk> createMockChunks() {
        DocumentChunk chunk1 = DocumentChunk.builder()
                .id(1L)
                .documentId(1L)
                .chunkIndex(0)
                .textContent("Java是一种广泛使用的编程语言，具有跨平台、面向对象等特点。")
                .build();

        DocumentChunk chunk2 = DocumentChunk.builder()
                .id(2L)
                .documentId(1L)
                .chunkIndex(1)
                .textContent("Java的核心特性包括：自动内存管理、强类型检查、多线程支持等。")
                .build();

        return Arrays.asList(chunk1, chunk2);
    }

    private TeachingDocument createMockDocument(TeachingDocument.DocumentType type) {
        return createMockDocument(type, TeachingDocument.DocumentStatus.PUBLISHED);
    }

    private TeachingDocument createMockDocument(TeachingDocument.DocumentType type,
                                               TeachingDocument.DocumentStatus status) {
        return createMockDocument(1L, TEST_USER_ID, type, status);
    }

    private TeachingDocument createMockDocument(String title, TeachingDocument.DocumentType type) {
        return TeachingDocument.builder()
                .id(1L)
                .title(title)
                .userId(TEST_USER_ID)
                .documentType(type)
                .status(TeachingDocument.DocumentStatus.PUBLISHED)
                .build();
    }

    private TeachingDocument createMockDocument(Long id, String userId) {
        return TeachingDocument.builder()
                .id(id)
                .title("测试文档")
                .userId(userId)
                .documentType(TeachingDocument.DocumentType.LESSON)
                .status(TeachingDocument.DocumentStatus.PUBLISHED)
                .build();
    }

    private TeachingDocument createMockDocument(Long id, String userId,
                                               TeachingDocument.DocumentType type,
                                               TeachingDocument.DocumentStatus status) {
        return TeachingDocument.builder()
                .id(id)
                .title("测试文档")
                .userId(userId)
                .documentType(type)
                .status(status)
                .build();
    }
}
