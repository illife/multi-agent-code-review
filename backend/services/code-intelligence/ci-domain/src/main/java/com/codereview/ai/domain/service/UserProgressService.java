package com.codereview.ai.domain.service;

import com.codereview.ai.domain.model.UserProgress;

import java.util.Map;

/**
 * User Progress Service Interface
 *
 * Provides user progress tracking functionality
 *
 * @author Code Review AI Team
 */
public interface UserProgressService {

    /**
     * Start a learning path
     *
     * @param userId User ID
     * @param learningPathId Learning Path ID
     * @return Created or updated progress
     */
    UserProgress startContent(Long userId, Long learningPathId);

    /**
     * Update progress for learning path
     *
     * @param userId User ID
     * @param learningPathId Learning Path ID
     * @param percent Progress percent (0-100)
     * @param currentSection Current section number
     * @return Updated progress
     */
    UserProgress updateProgress(Long userId, Long learningPathId, int percent, int currentSection);

    /**
     * Mark learning path as completed
     *
     * @param userId User ID
     * @param learningPathId Learning Path ID
     * @param score Optional score
     * @return Updated progress
     */
    UserProgress completeContent(Long userId, Long learningPathId, Integer score);

    /**
     * Get user's overall learning statistics
     *
     * @param userId User ID
     * @return Statistics map
     */
    Map<String, Object> getUserOverallStats(Long userId);

    /**
     * Get user's progress for specific learning path
     *
     * @param userId User ID
     * @param learningPathId Learning Path ID
     * @return Progress or null if not started
     */
    UserProgress getContentProgress(Long userId, Long learningPathId);

    /**
     * Get user's in-progress learning paths
     *
     * @param userId User ID
     * @return List of in-progress items
     */
    Map<String, Object> getInProgressContent(Long userId);

    /**
     * Get user's completed learning paths
     *
     * @param userId User ID
     * @return List of completed items
     */
    Map<String, Object> getCompletedContent(Long userId);

    /**
     * Add time spent to learning path
     *
     * @param userId User ID
     * @param learningPathId Learning Path ID
     * @param minutes Minutes to add
     * @return Updated progress
     */
    UserProgress addTimeSpent(Long userId, Long learningPathId, int minutes);

    /**
     * Reset progress for learning path
     *
     * @param userId User ID
     * @param learningPathId Learning Path ID
     * @return Reset progress
     */
    UserProgress resetProgress(Long userId, Long learningPathId);

    /**
     * Delete progress record
     *
     * @param userId User ID
     * @param learningPathId Learning Path ID
     */
    void deleteProgress(Long userId, Long learningPathId);

    /**
     * Get recently accessed learning paths
     *
     * @param userId User ID
     * @param days Number of days to look back
     * @return List of recently accessed learning paths
     */
    Map<String, Object> getRecentlyAccessed(Long userId, int days);
}
