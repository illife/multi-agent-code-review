package com.company.kb.controller;

import com.company.kb.api.dto.PageResponse;
import com.company.kb.core.domain.TeachingDocument;
import com.company.kb.core.service.TeachingService;
import com.think.platform.shared.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 教学文档控制器
 * 提供教学MD文档生成、查询、管理API
 */
@Slf4j
@RestController
@RequestMapping("/teaching")
@RequiredArgsConstructor
public class TeachingController {

    private final TeachingService teachingService;

    /**
     * 生成教学文档
     *
     * @param request 生成请求
     * @return 生成的教学文档
     */
    @PostMapping("/generate")
    public Result<TeachingDocument> generateTeachingDocument(
            @RequestBody GenerateTeachingRequest request,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            log.info("生成教学文档: username={}, type={}, title={}", username, request.getDocumentType(), request.getTitle());

            TeachingDocument document = teachingService.generateTeachingDocument(
                    username,
                    request.getDocumentType(),
                    request.getTestResultId(),
                    request.getKnowledgePointIds(),
                    request.getTitle(),
                    username
            );

            return Result.success(document);

        } catch (Exception e) {
            log.error("生成教学文档失败", e);
            return Result.failed(500, "生成教学文档失败: " + e.getMessage());
        }
    }

    /**
     * 生成个性化教学文档（基于薄弱知识点）
     *
     * @param request 请求参数
     * @return 生成的教学文档
     */
    @PostMapping("/personalized")
    public Result<TeachingDocument> generatePersonalizedLesson(
            @RequestBody PersonalizedLessonRequest request,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            log.info("生成个性化教学文档: username={}, weakPoints={}", username, request.getWeakPoints());

            TeachingDocument document = teachingService.generatePersonalizedLesson(
                    username,
                    request.getTestResultId(),
                    request.getWeakPoints(),
                    username
            );

            return Result.success(document);

        } catch (Exception e) {
            log.error("生成个性化教学文档失败", e);
            return Result.failed(500, "生成个性化教学文档失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户教学文档列表
     *
     * @param page 页码
     * @param size 每页大小
     * @return 文档列表
     */
    @GetMapping("/documents")
    public Result<PageResponse<TeachingDocument>> getUserDocuments(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            List<TeachingDocument> documents = teachingService.getUserTeachingDocuments(username);

            // 手动分页
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), documents.size());
            List<TeachingDocument> pagedDocuments = start < documents.size()
                    ? documents.subList(start, end)
                    : List.of();

            PageResponse<TeachingDocument> response = new PageResponse<>(
                    pagedDocuments,
                    documents.size(),
                    page,
                    (int) Math.ceil((double) documents.size() / size),
                    size,
                    end < documents.size(),
                    page > 0,
                    page == 0,
                    end >= documents.size()
            );

            return Result.success(response);

        } catch (Exception e) {
            log.error("获取教学文档列表失败", e);
            return Result.failed(500, "获取教学文档列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取已发布的教学文档
     *
     * @return 已发布文档列表
     */
    @GetMapping("/published")
    public Result<List<TeachingDocument>> getPublishedDocuments(Authentication authentication) {
        try {
            String username = authentication.getName();
            List<TeachingDocument> documents = teachingService.getPublishedTeachingDocuments(username);
            return Result.success(documents);

        } catch (Exception e) {
            log.error("获取已发布教学文档失败", e);
            return Result.failed(500, "获取已发布教学文档失败: " + e.getMessage());
        }
    }

    /**
     * 搜索教学文档
     *
     * @param keyword 关键词
     * @return 搜索结果
     */
    @GetMapping("/search")
    public Result<List<TeachingDocument>> searchDocuments(
            @RequestParam("keyword") String keyword,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            List<TeachingDocument> documents = teachingService.searchTeachingDocuments(username, keyword);
            return Result.success(documents);

        } catch (Exception e) {
            log.error("搜索教学文档失败: keyword={}", keyword, e);
            return Result.failed(500, "搜索教学文档失败: " + e.getMessage());
        }
    }

    /**
     * 删除教学文档
     *
     * @param documentId 文档ID
     * @return 删除结果
     */
    @DeleteMapping("/documents/{documentId}")
    public Result<String> deleteDocument(
            @PathVariable Long documentId,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            teachingService.deleteTeachingDocument(documentId, username);
            return Result.success("教学文档删除成功");

        } catch (IllegalArgumentException e) {
            return Result.failed(400, e.getMessage());
        } catch (Exception e) {
            log.error("删除教学文档失败: documentId={}", documentId, e);
            return Result.failed(500, "删除教学文档失败: " + e.getMessage());
        }
    }

    /**
     * 获取教学文档统计信息
     *
     * @return 统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(Authentication authentication) {
        try {
            String username = authentication.getName();
            List<TeachingDocument> allDocuments = teachingService.getUserTeachingDocuments(username);

            Map<String, Object> stats = new HashMap<>();
            stats.put("total", allDocuments.size());
            stats.put("published", allDocuments.stream().filter(d -> d.getStatus() == TeachingDocument.DocumentStatus.PUBLISHED).count());
            stats.put("draft", allDocuments.stream().filter(d -> d.getStatus() == TeachingDocument.DocumentStatus.DRAFT).count());

            // 按类型统计
            Map<String, Long> byType = new HashMap<>();
            for (TeachingDocument.DocumentType type : TeachingDocument.DocumentType.values()) {
                long count = allDocuments.stream().filter(d -> d.getDocumentType() == type).count();
                if (count > 0) {
                    byType.put(type.name(), count);
                }
            }
            stats.put("byType", byType);

            return Result.success(stats);

        } catch (Exception e) {
            log.error("获取教学文档统计失败", e);
            return Result.failed(500, "获取教学文档统计失败: " + e.getMessage());
        }
    }

    /**
     * 生成教学文档请求
     */
    public static class GenerateTeachingRequest {
        private TeachingDocument.DocumentType documentType;
        private Long testResultId;
        private List<String> knowledgePointIds;
        private String title;

        public TeachingDocument.DocumentType getDocumentType() {
            return documentType;
        }

        public void setDocumentType(TeachingDocument.DocumentType documentType) {
            this.documentType = documentType;
        }

        public Long getTestResultId() {
            return testResultId;
        }

        public void setTestResultId(Long testResultId) {
            this.testResultId = testResultId;
        }

        public List<String> getKnowledgePointIds() {
            return knowledgePointIds;
        }

        public void setKnowledgePointIds(List<String> knowledgePointIds) {
            this.knowledgePointIds = knowledgePointIds;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    /**
     * 个性化教学请求
     */
    public static class PersonalizedLessonRequest {
        private Long testResultId;
        private List<String> weakPoints;

        public Long getTestResultId() {
            return testResultId;
        }

        public void setTestResultId(Long testResultId) {
            this.testResultId = testResultId;
        }

        public List<String> getWeakPoints() {
            return weakPoints;
        }

        public void setWeakPoints(List<String> weakPoints) {
            this.weakPoints = weakPoints;
        }
    }
}
