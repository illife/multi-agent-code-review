package com.codereview.ai.domain.service;

import com.codereview.ai.domain.model.Achievement;
import com.codereview.ai.domain.model.UserAchievement;

import java.util.List;
import java.util.Map;

/**
 * Achievement Service Interface
 *
 * Provides achievement and gamification functionality
 *
 * @author Code Review AI Team
 */
public interface AchievementService {

    /**
     * Unlock achievement for user
     *
     * @param userId User ID
     * @param achievementId Achievement ID
     * @return User achievement record
     */
    UserAchievement unlockAchievement(Long userId, Long achievementId);

    /**
     * Unlock achievement by code
     *
     * @param userId User ID
     * @param achievementCode Achievement code
     * @return User achievement record or null if already unlocked
     */
    UserAchievement unlockAchievementByCode(Long userId, String achievementCode);

    /**
     * Get user's achievements
     *
     * @param userId User ID
     * @return List of user achievements
     */
    List<UserAchievement> getUserAchievements(Long userId);

    /**
     * Get user's achievements with details
     *
     * @param userId User ID
     * @return List with achievement details
     */
    List<Map<String, Object>> getUserAchievementsWithDetails(Long userId);

    /**
     * Get all available achievements
     *
     * @param category Filter by category (optional)
     * @return List of achievements
     */
    List<Achievement> getAllAchievements(String category);

    /**
     * Check and unlock achievements based on event
     *
     * @param userId User ID
     * @param eventType Event type (e.g., "review_completed", "lesson_completed", "exercise_passed")
     * @param eventData Event data for evaluation
     * @return List of newly unlocked achievements
     */
    List<UserAchievement> checkAndUnlockAchievements(Long userId, String eventType, Map<String, Object> eventData);

    /**
     * Get leaderboard
     *
     * @param achievementCode Achievement code (optional, for specific achievement)
     * @param limit Limit results
     * @return Leaderboard entries
     */
    List<Map<String, Object>> getLeaderboard(String achievementCode, int limit);

    /**
     * Get XP leaderboard
     *
     * @param limit Limit results
     * @return Leaderboard entries
     */
    List<Map<String, Object>> getXpLeaderboard(int limit);

    /**
     * Get user's achievement progress
     *
     * @param userId User ID
     * @return Progress map for each achievement type
     */
    Map<String, Object> getUserAchievementProgress(Long userId);

    /**
     * Get user's total XP
     *
     * @param userId User ID
     * @return Total XP earned
     */
    Long getUserTotalXp(Long userId);

    /**
     * Get recently earned achievements
     *
     * @param userId User ID
     * @param days Number of days to look back
     * @return List of recently earned achievements
     */
    List<Map<String, Object>> getRecentAchievements(Long userId, int days);

    /**
     * Get achievement by code
     *
     * @param code Achievement code
     * @return Achievement
     */
    Achievement getAchievementByCode(String code);

    /**
     * Create new achievement
     *
     * @param achievement Achievement to create
     * @return Created achievement
     */
    Achievement createAchievement(Achievement achievement);

    /**
     * Update achievement
     *
     * @param achievementId Achievement ID
     * @param achievement Updated achievement data
     * @return Updated achievement
     */
    Achievement updateAchievement(Long achievementId, Achievement achievement);

    /**
     * Check if user has earned specific achievement
     *
     * @param userId User ID
     * @param achievementCode Achievement code
     * @return True if earned
     */
    boolean hasAchievement(Long userId, String achievementCode);

    /**
     * Get achievement categories with counts
     *
     * @return Category counts
     */
    Map<String, Object> getAchievementCategories();
}
