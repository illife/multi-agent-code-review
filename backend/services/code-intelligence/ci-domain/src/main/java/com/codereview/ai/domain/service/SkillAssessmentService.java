package com.codereview.ai.domain.service;

import com.codereview.ai.domain.model.CodeReview;
import com.codereview.ai.domain.model.SkillProfile;

import java.util.List;
import java.util.Map;

/**
 * Skill Assessment Service Interface
 *
 * Provides user skill assessment and tracking functionality
 *
 * @author Code Review AI Team
 */
public interface SkillAssessmentService {

    /**
     * Get or create user skill profile
     *
     * @param userId User ID
     * @param language Programming language
     * @param category Skill category (e.g., "security", "performance", "best-practices")
     * @return Skill profile
     */
    SkillProfile getSkillProfile(Long userId, String language, String category);

    /**
     * Get user's skill levels for a language
     *
     * @param userId User ID
     * @param language Programming language
     * @return List of skill profiles
     */
    List<SkillProfile> getUserSkillLevels(Long userId, String language);

    /**
     * Get all skill profiles for user
     *
     * @param userId User ID
     * @return List of all skill profiles
     */
    List<SkillProfile> getAllUserSkills(Long userId);

    /**
     * Assess and update skills from code review
     *
     * @param userId User ID
     * @param review Completed code review
     * @return Updated skill profiles
     */
    List<SkillProfile> assessFromReview(Long userId, CodeReview review);

    /**
     * Assess and update skills from exercise completion
     *
     * @param userId User ID
     * @param language Programming language
     * @param category Exercise category
     * @param passed Whether exercise was passed
     * @return Updated skill profile
     */
    SkillProfile assessFromExercise(Long userId, String language, String category, boolean passed);

    /**
     * Get improvement recommendations
     *
     * @param userId User ID
     * @param language Programming language
     * @return Map of recommendations
     */
    Map<String, Object> getImprovementResources(Long userId, String language);

    /**
     * Add XP to user's skill profile
     *
     * @param userId User ID
     * @param language Programming language
     * @param category Skill category
     * @param xp XP to add
     * @return Updated skill profile
     */
    SkillProfile addXp(Long userId, String language, String category, int xp);

    /**
     * Get user's overall skill summary
     *
     * @param userId User ID
     * @return Summary map
     */
    Map<String, Object> getUserSkillSummary(Long userId);

    /**
     * Get leaderboard for specific skill
     *
     * @param language Programming language
     * @param category Skill category
     * @param limit Limit results
     * @return Leaderboard entries
     */
    List<Map<String, Object>> getLeaderboard(String language, String category, int limit);

    /**
     * Get user's skill level category
     *
     * @param userId User ID
     * @param language Programming language
     * @param category Skill category
     * @return Skill category (NOVICE, BEGINNER, INTERMEDIATE, ADVANCED, EXPERT)
     */
    String getSkillCategory(Long userId, String language, String category);

    /**
     * Initialize skill profile for new user
     *
     * @param userId User ID
     * @param languages List of languages to initialize
     */
    void initializeUserSkills(Long userId, List<String> languages);

    /**
     * Get recommended content based on skill gaps
     *
     * @param userId User ID
     * @param language Programming language
     * @return Recommended content
     */
    Map<String, Object> getRecommendedContentForSkillGaps(Long userId, String language);
}
