package com.codereview.ai.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Teaching Content Service Interface
 *
 * NOTE: This interface uses Object types instead of LearningContent
 * because the LearningContent entity is not available.
 * This service needs to be reimplemented when LearningContent is available.
 *
 * @author Code Review AI Team
 */
public interface TeachingContentService {

    /**
     * Create new learning content
     *
     * @param content Content to create
     * @param creatorId User ID of content creator
     * @return Created content
     */
    Object createContent(Object content, Long creatorId);

    /**
     * Update existing content
     *
     * @param contentId Content ID
     * @param content Updated content data
     * @return Updated content
     */
    Object updateContent(Long contentId, Object content);

    /**
     * Get content by ID
     *
     * @param contentId Content ID
     * @return Content
     */
    Object getContentById(Long contentId);

    /**
     * List content with pagination and filters
     *
     * @param language Filter by language (optional)
     * @param category Filter by category (optional)
     * @param difficulty Filter by difficulty (optional)
     * @param contentType Filter by content type (optional)
     * @param pageable Pagination parameters
     * @return Paginated content list
     */
    Page<Object> listContent(String language, String category,
                             Object difficulty,
                             Object contentType,
                             Pageable pageable);

    /**
     * Get recommended content for user
     *
     * @param userId User ID
     * @param language Programming language
     * @return Recommended content list
     */
    List<Object> getRecommendedContent(Long userId, String language);

    /**
     * Publish content
     *
     * @param contentId Content ID
     * @return Published content
     */
    Object publishContent(Long contentId);

    /**
     * Unpublish content
     *
     * @param contentId Content ID
     * @return Unpublished content
     */
    Object unpublishContent(Long contentId);

    /**
     * Delete content
     *
     * @param contentId Content ID
     * @param userId User ID (for permission check)
     */
    void deleteContent(Long contentId, Long userId);

    /**
     * Get content by creator
     *
     * @param creatorId Creator user ID
     * @param pageable Pagination parameters
     * @return Paginated content list
     */
    Page<Object> getCreatorContent(Long creatorId, Pageable pageable);

    /**
     * Search content by keyword
     *
     * @param keyword Search keyword
     * @param pageable Pagination parameters
     * @return Paginated content list
     */
    Page<Object> searchContent(String keyword, Pageable pageable);
}
