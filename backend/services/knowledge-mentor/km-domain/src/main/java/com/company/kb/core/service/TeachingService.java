package com.company.kb.core.service;

import com.company.kb.core.domain.Document;
import com.company.kb.core.domain.DocumentChunk;
import com.company.kb.core.domain.TeachingDocument;
import com.company.kb.core.repository.DocumentChunkRepository;
import com.company.kb.core.repository.DocumentRepository;
import com.company.kb.core.repository.TeachingDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 教学服务
 * 根据测试结果和知识点生成教学MD文档
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TeachingService {

    private final TeachingDocumentRepository teachingDocumentRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;

    /**
     * 生成教学文档
     *
     * @param userId 用户ID
     * @param documentType 文档类型
     * @param testResultId 测试结果ID
     * @param knowledgePointIds 知识点ID列表
     * @param title 文档标题
     * @param createdBy 创建者
     * @return 生成的教学文档
     */
    @Transactional
    public TeachingDocument generateTeachingDocument(
            String userId,
            TeachingDocument.DocumentType documentType,
            Long testResultId,
            List<String> knowledgePointIds,
            String title,
            String createdBy
    ) {
        log.info("开始生成教学文档: userId={}, type={}, title={}", userId, documentType, title);

        try {
            // 1. 获取相关知识点内容
            String knowledgeContent = getKnowledgeContent(knowledgePointIds);

            // 2. 生成Markdown文档
            String markdownContent = generateMarkdown(documentType, title, knowledgeContent);

            // 3. 创建教学文档记录
            TeachingDocument document = TeachingDocument.builder()
                    .title(title)
                    .userId(userId)
                    .documentType(documentType)
                    .knowledgePointIds(String.join(",", knowledgePointIds))
                    .testResultId(testResultId)
                    .content(markdownContent)
                    .status(TeachingDocument.DocumentStatus.PUBLISHED)
                    .priority(2)
                    .tags(generateTags(documentType))
                    .createdBy(createdBy)
                    .build();

            document = teachingDocumentRepository.save(document);
            log.info("教学文档生成成功: documentId={}", document.getId());

            return document;

        } catch (Exception e) {
            log.error("生成教学文档失败", e);
            throw new RuntimeException("生成教学文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 基于测试结果生成个性化教学文档
     *
     * @param userId 用户ID
     * @param testResultId 测试结果ID
     * @param weakPoints 薄弱知识点列表
     * @param createdBy 创建者
     * @return 生成的教学文档
     */
    @Transactional
    public TeachingDocument generatePersonalizedLesson(
            String userId,
            Long testResultId,
            List<String> weakPoints,
            String createdBy
    ) {
        log.info("开始生成个性化教学文档: userId={}, weakPoints={}", userId, weakPoints);

        try {
            // 1. 获取薄弱知识点相关内容
            List<DocumentChunk> relevantChunks = getRelevantChunks(weakPoints);

            // 2. 生成个性化教学内容
            String personalizedContent = generatePersonalizedContent(weakPoints, relevantChunks);

            // 3. 创建教学文档
            String title = "个性化教学 - " + LocalDateTime.now().toLocalDate().toString();
            TeachingDocument document = TeachingDocument.builder()
                    .title(title)
                    .userId(userId)
                    .documentType(TeachingDocument.DocumentType.LESSON)
                    .knowledgePointIds(String.join(",", weakPoints))
                    .testResultId(testResultId)
                    .content(personalizedContent)
                    .status(TeachingDocument.DocumentStatus.PUBLISHED)
                    .priority(1)
                    .tags("个性化,薄弱知识点,补习")
                    .createdBy(createdBy)
                    .build();

            document = teachingDocumentRepository.save(document);
            log.info("个性化教学文档生成成功: documentId={}", document.getId());

            return document;

        } catch (Exception e) {
            log.error("生成个性化教学文档失败", e);
            throw new RuntimeException("生成个性化教学文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取知识内容
     */
    private String getKnowledgeContent(List<String> knowledgePointIds) {
        if (knowledgePointIds == null || knowledgePointIds.isEmpty()) {
            return "";
        }

        // 根据知识点ID搜索相关文档块
        List<String> contents = new ArrayList<>();
        for (String pointId : knowledgePointIds) {
            // 这里可以扩展为从专门的KnowledgePoint表获取
            // 目前简单实现：从DocumentChunk中搜索相关内容
            List<DocumentChunk> chunks = documentChunkRepository.findByDocumentId(Long.parseLong(pointId));
            chunks.forEach(chunk -> contents.add(chunk.getTextContent()));
        }

        return String.join("\n\n", contents);
    }

    /**
     * 获取相关文档块
     */
    private List<DocumentChunk> getRelevantChunks(List<String> knowledgePointIds) {
        List<DocumentChunk> chunks = new ArrayList<>();
        for (String pointId : knowledgePointIds) {
            try {
                chunks.addAll(documentChunkRepository.findByDocumentId(Long.parseLong(pointId)));
            } catch (Exception e) {
                log.warn("获取知识点内容失败: pointId={}", pointId);
            }
        }
        return chunks;
    }

    /**
     * 生成Markdown格式教学内容
     */
    private String generateMarkdown(TeachingDocument.DocumentType type, String title, String knowledgeContent) {
        StringBuilder markdown = new StringBuilder();

        // 标题
        markdown.append("# ").append(title).append("\n\n");
        markdown.append("**生成时间**: ").append(LocalDateTime.now()).append("\n\n");
        markdown.append("---\n\n");

        // 根据文档类型生成不同内容
        switch (type) {
            case LESSON -> generateLessonContent(markdown, knowledgeContent);
            case PRACTICE -> generatePracticeContent(markdown, knowledgeContent);
            case REVIEW -> generateReviewContent(markdown, knowledgeContent);
            case KNOWLEDGE_GAP -> generateKnowledgeGapContent(markdown, knowledgeContent);
            default -> markdown.append(knowledgeContent);
        }

        return markdown.toString();
    }

    /**
     * 生成课时内容
     */
    private void generateLessonContent(StringBuilder markdown, String knowledgeContent) {
        markdown.append("## 学习目标\n\n");
        markdown.append("完成本课时后，你将能够：\n");
        markdown.append("- 理解核心概念\n");
        markdown.append("- 掌握实际应用\n");
        markdown.append("- 能够解决相关问题\n\n");

        markdown.append("## 知识要点\n\n");
        markdown.append(knowledgeContent).append("\n\n");

        markdown.append("## 思考题\n\n");
        markdown.append("1. 本课核心概念是什么？\n");
        markdown.append("2. 如何将所学知识应用到实际场景？\n");
        markdown.append("3. 还有哪些疑问需要进一步探讨？\n\n");
    }

    /**
     * 生成练习内容
     */
    private void generatePracticeContent(StringBuilder markdown, String knowledgeContent) {
        markdown.append("## 练习题\n\n");

        // 基于知识点生成练习题
        if (knowledgeContent != null && !knowledgeContent.isEmpty()) {
            String[] paragraphs = knowledgeContent.split("\n\n");
            for (int i = 0; i < Math.min(paragraphs.length, 5); i++) {
                markdown.append("### 练习 ").append(i + 1).append("\n\n");
                markdown.append("基于以下内容完成练习：\n\n");
                markdown.append("> ").append(paragraphs[i].substring(0, Math.min(100, paragraphs[i].length()))).append("...\n\n");
                markdown.append("**要求**:\n");
                markdown.append("- 理解并应用概念\n");
                markdown.append("- 写出详细解答过程\n");
                markdown.append("- 标注关键步骤\n\n");
            }
        }

        markdown.append("## 解析与答案\n\n");
        markdown.append("*请在完成练习后查看答案解析*\n");
    }

    /**
     * 生成复习内容
     */
    private void generateReviewContent(StringBuilder markdown, String knowledgeContent) {
        markdown.append("## 复习要点\n\n");
        markdown.append(knowledgeContent).append("\n\n");

        markdown.append("## 重点记忆\n\n");
        markdown.append("- 要点一\n");
        markdown.append("- 要点二\n");
        markdown.append("- 要点三\n\n");

        markdown.append("## 常见错误\n\n");
        markdown.append("⚠️ **注意**: 以下为常见错误，请避免：\n\n");
        markdown.append("- 错误理解一\n");
        markdown.append("- 错误理解二\n");
        markdown.append("- 正确理解：[待补充]\n\n");
    }

    /**
     * 生成知识缺口分析内容
     */
    private void generateKnowledgeGapContent(StringBuilder markdown, String knowledgeContent) {
        markdown.append("## 知识缺口分析\n\n");

        markdown.append("### 当前水平评估\n\n");
        markdown.append("根据测试结果分析，在以下方面存在提升空间：\n\n");

        markdown.append("### 改进建议\n\n");
        markdown.append("#### 短期目标（1-2周）\n\n");
        markdown.append("- [ ] 补强基础知识\n");
        markdown.append("- [ ] 完成练习题\n");
        markdown.append("- [ ] 查阅相关资料\n\n");

        markdown.append("#### 中期目标（1-2个月）\n\n");
        markdown.append("- [ ] 掌握进阶概念\n");
        markdown.append("- [ ] 完成项目实战\n");
        markdown.append("- [ ] 参与讨论交流\n\n");

        markdown.append("### 推荐学习资源\n\n");
        markdown.append("1. 系统文档\n");
        markdown.append("2. 视频教程\n");
        markdown.append("3. 实战项目\n");
    }

    /**
     * 生成个性化教学内容
     */
    private String generatePersonalizedContent(List<String> weakPoints, List<DocumentChunk> relevantChunks) {
        StringBuilder content = new StringBuilder();

        content.append("# 个性化学习计划\n\n");
        content.append("**生成时间**: ").append(LocalDateTime.now()).append("\n\n");
        content.append("---\n\n");

        // 薄弱知识点分析
        content.append("## 薄弱知识点分析\n\n");
        for (int i = 0; i < weakPoints.size(); i++) {
            content.append(i + 1).append(". ").append(weakPoints.get(i)).append("\n");
        }
        content.append("\n");

        // 推荐学习内容
        content.append("## 推荐学习内容\n\n");
        if (!relevantChunks.isEmpty()) {
            for (int i = 0; i < Math.min(relevantChunks.size(), 5); i++) {
                DocumentChunk chunk = relevantChunks.get(i);
                content.append("### ").append(i + 1).append("\n\n");
                content.append(chunk.getTextContent()).append("\n\n");
            }
        }

        // 学习计划
        content.append("## 学习计划\n\n");
        content.append("### 第一阶段（第1-2天）\n\n");
        content.append("- [ ] 学习基础概念\n");
        content.append("- [ ] 完成练习题\n");
        content.append("- [ ] 整理笔记\n\n");

        content.append("### 第二阶段（第3-5天）\n\n");
        content.append("- [ ] 深入理解原理\n");
        content.append("- [ ] 做项目实战\n");
        content.append("- [ ] 参加讨论\n\n");

        content.append("### 第三阶段（第6-7天）\n\n");
        content.append("- [ ] 复习巩固\n");
        content.append("- [ ] 模拟测试\n");
        content.append("- [ ] 总结提升\n\n");

        return content.toString();
    }

    /**
     * 生成标签
     */
    private String generateTags(TeachingDocument.DocumentType type) {
        List<String> tags = new ArrayList<>();
        tags.add("教学");

        switch (type) {
            case LESSON:
                tags.add("课时");
                tags.add("学习");
                break;
            case PRACTICE:
                tags.add("练习");
                tags.add("作业");
                break;
            case REVIEW:
                tags.add("复习");
                tags.add("总结");
                break;
            case KNOWLEDGE_GAP:
                tags.add("知识缺口");
                tags.add("补习");
                break;
        }

        return String.join(",", tags);
    }

    /**
     * 获取用户的教学文档列表
     */
    public List<TeachingDocument> getUserTeachingDocuments(String userId) {
        return teachingDocumentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 获取已发布的教学文档
     */
    public List<TeachingDocument> getPublishedTeachingDocuments(String userId) {
        return teachingDocumentRepository.findPublishedByUserId(userId);
    }

    /**
     * 搜索教学文档
     */
    public List<TeachingDocument> searchTeachingDocuments(String userId, String keyword) {
        return teachingDocumentRepository.searchByKeyword(userId, keyword);
    }

    /**
     * 删除教学文档
     */
    @Transactional
    public void deleteTeachingDocument(Long documentId, String userId) {
        TeachingDocument document = teachingDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("教学文档不存在: " + documentId));

        if (!document.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权删除此文档");
        }

        teachingDocumentRepository.delete(document);
    }
}
