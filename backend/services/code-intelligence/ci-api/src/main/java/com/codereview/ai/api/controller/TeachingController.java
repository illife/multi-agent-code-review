package com.codereview.ai.api.controller;

import com.codereview.ai.api.service.UserService;
import com.think.platform.shared.common.result.Result;
import com.think.platform.shared.common.result.ResultCode;
import com.codereview.ai.domain.model.*;
import com.codereview.ai.domain.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Teaching Controller
 *
 * Provides REST API endpoints for teaching and learning features
 *
 * @author Code Review AI Team
 */
@Slf4j
@RestController
@RequestMapping("/api/teaching")
@RequiredArgsConstructor
public class TeachingController {

    // Commented out due to missing TeachingContentService and LearningContent dependencies
    // private final TeachingContentService teachingContentService;
    private final UserProgressService userProgressService;
    private final SkillAssessmentService skillAssessmentService;
    private final ExerciseService exerciseService;
    private final AchievementService achievementService;
    private final UserService userService;

    // ================================================================
    // Teaching Content Endpoints
    // Commented out due to missing LearningContent entity and TeachingContentService
    // ================================================================

    /*
    @PostMapping("/lessons")
    public Result<LearningContent> createLesson(
            @RequestBody CreateContentRequest request,
            Authentication authentication) {
        // Implementation requires LearningContent entity
        return Result.error("Feature temporarily unavailable");
    }

    @GetMapping("/lessons")
    public Result<Map<String, Object>> listLessons(
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) LearningContent.DifficultyLevel difficulty,
            @RequestParam(required = false) LearningContent.ContentType contentType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        // Implementation requires LearningContent entity
        return Result.error("Feature temporarily unavailable");
    }

    @GetMapping("/lessons/{id}")
    public Result<LearningContent> getLesson(@PathVariable Long id) {
        // Implementation requires LearningContent entity
        return Result.error("Feature temporarily unavailable");
    }

    @PutMapping("/lessons/{id}")
    public Result<LearningContent> updateLesson(
            @PathVariable Long id,
            @RequestBody CreateContentRequest request,
            Authentication authentication) {
        // Implementation requires LearningContent entity
        return Result.error("Feature temporarily unavailable");
    }

    @GetMapping("/lessons/recommended")
    public Result<List<LearningContent>> getRecommendedLessons(
            @RequestParam String language,
            Authentication authentication) {
        // Implementation requires LearningContent entity
        return Result.error("Feature temporarily unavailable");
    }

    @PostMapping("/lessons/{id}/publish")
    public Result<LearningContent> publishLesson(@PathVariable Long id) {
        // Implementation requires LearningContent entity
        return Result.error("Feature temporarily unavailable");
    }
    */

    // ================================================================
    // Progress Tracking Endpoints
    // ================================================================

    @GetMapping("/progress")
    public Result<Map<String, Object>> getUserOverallProgress(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuth(authentication);
            Map<String, Object> stats = userProgressService.getUserOverallStats(userId);

            return Result.success(stats);

        } catch (Exception e) {
            log.error("Failed to get user progress", e);
            return Result.error("Failed to get progress: " + e.getMessage());
        }
    }

    @PostMapping("/progress/{learningPathId}/start")
    public Result<UserProgress> startContent(
            @PathVariable Long learningPathId,
            Authentication authentication) {

        try {
            Long userId = getUserIdFromAuth(authentication);
            UserProgress progress = userProgressService.startContent(userId, learningPathId);

            return Result.success(progress);

        } catch (Exception e) {
            log.error("Failed to start learning path", e);
            return Result.error("Failed to start learning path: " + e.getMessage());
        }
    }

    @PutMapping("/progress/{learningPathId}/update")
    public Result<UserProgress> updateProgress(
            @PathVariable Long learningPathId,
            @RequestBody UpdateProgressRequest request,
            Authentication authentication) {

        try {
            Long userId = getUserIdFromAuth(authentication);
            UserProgress progress = userProgressService.updateProgress(
                    userId, learningPathId, request.getPercent(), request.getCurrentSection());

            return Result.success(progress);

        } catch (Exception e) {
            log.error("Failed to update progress", e);
            return Result.error("Failed to update progress: " + e.getMessage());
        }
    }

    @PostMapping("/progress/{learningPathId}/complete")
    public Result<UserProgress> completeContent(
            @PathVariable Long learningPathId,
            @RequestBody CompleteContentRequest request,
            Authentication authentication) {

        try {
            Long userId = getUserIdFromAuth(authentication);
            UserProgress progress = userProgressService.completeContent(userId, learningPathId, request.getScore());

            return Result.success(progress);

        } catch (Exception e) {
            log.error("Failed to complete learning path", e);
            return Result.error("Failed to complete learning path: " + e.getMessage());
        }
    }

    // ================================================================
    // Skill Assessment Endpoints
    // ================================================================

    @GetMapping("/skills")
    public Result<List<SkillProfile>> getUserSkills(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuth(authentication);
            List<SkillProfile> skills = skillAssessmentService.getAllUserSkills(userId);

            return Result.success(skills);

        } catch (Exception e) {
            log.error("Failed to get user skills", e);
            return Result.error("Failed to get skills: " + e.getMessage());
        }
    }

    @GetMapping("/skills/{language}")
    public Result<List<SkillProfile>> getUserSkillsByLanguage(
            @PathVariable String language,
            Authentication authentication) {

        try {
            Long userId = getUserIdFromAuth(authentication);
            List<SkillProfile> skills = skillAssessmentService.getUserSkillLevels(userId, language);

            return Result.success(skills);

        } catch (Exception e) {
            log.error("Failed to get user skills by language", e);
            return Result.error("Failed to get skills: " + e.getMessage());
        }
    }

    @GetMapping("/skills/summary")
    public Result<Map<String, Object>> getUserSkillSummary(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuth(authentication);
            Map<String, Object> summary = skillAssessmentService.getUserSkillSummary(userId);

            return Result.success(summary);

        } catch (Exception e) {
            log.error("Failed to get skill summary", e);
            return Result.error("Failed to get skill summary: " + e.getMessage());
        }
    }

    @GetMapping("/skills/recommendations")
    public Result<Map<String, Object>> getSkillRecommendations(
            @RequestParam String language,
            Authentication authentication) {

        try {
            Long userId = getUserIdFromAuth(authentication);
            Map<String, Object> recommendations = skillAssessmentService.getImprovementResources(userId, language);

            return Result.success(recommendations);

        } catch (Exception e) {
            log.error("Failed to get skill recommendations", e);
            return Result.error("Failed to get recommendations: " + e.getMessage());
        }
    }

    // ================================================================
    // Exercise Endpoints
    // ================================================================

    @GetMapping("/exercises")
    public Result<Map<String, Object>> listExercises(
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Exercise.Difficulty difficulty,
            @RequestParam(required = false) String skillTag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            return Result.success(exerciseService.listExercises(
                    language, category, difficulty, skillTag, page, size));

        } catch (Exception e) {
            log.error("Failed to list exercises", e);
            return Result.error("Failed to list exercises: " + e.getMessage());
        }
    }

    @GetMapping("/exercises/{id}")
    public Result<Exercise> getExercise(@PathVariable Long id) {
        try {
            Exercise exercise = exerciseService.getExerciseById(id);
            return Result.success(exercise);
        } catch (IllegalArgumentException e) {
            return Result.error(ResultCode.NOT_FOUND.getCode(), "Exercise not found: " + id);
        } catch (Exception e) {
            log.error("Failed to get exercise", e);
            return Result.error("Failed to get exercise: " + e.getMessage());
        }
    }

    @PostMapping("/exercises/{id}/attempt")
    public Result<Map<String, Object>> submitExerciseAttempt(
            @PathVariable Long id,
            @RequestBody SubmitAttemptRequest request,
            Authentication authentication) {

        try {
            Long userId = getUserIdFromAuth(authentication);
            Map<String, Object> result = exerciseService.submitAttempt(
                    userId, id, request.getCode(), request.getTimeSpentSeconds());

            return Result.success(result);

        } catch (Exception e) {
            log.error("Failed to submit exercise attempt", e);
            return Result.error("Failed to submit attempt: " + e.getMessage());
        }
    }

    @GetMapping("/exercises/{id}/hints")
    public Result<String> getExerciseHints(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int hintNumber,
            Authentication authentication) {

        try {
            Long userId = getUserIdFromAuth(authentication);
            String hint = exerciseService.getHints(id, hintNumber, userId);

            if (hint == null) {
                return Result.error("No hint available at this number");
            }

            return Result.success(hint);

        } catch (Exception e) {
            log.error("Failed to get exercise hints", e);
            return Result.error("Failed to get hints: " + e.getMessage());
        }
    }

    @GetMapping("/exercises/recommended")
    public Result<List<Exercise>> getRecommendedExercises(
            @RequestParam String language,
            @RequestParam(defaultValue = "5") int count,
            Authentication authentication) {

        try {
            Long userId = getUserIdFromAuth(authentication);
            List<Exercise> recommended = exerciseService.getRecommendedExercises(userId, language, count);

            return Result.success(recommended);

        } catch (Exception e) {
            log.error("Failed to get recommended exercises", e);
            return Result.error("Failed to get recommendations: " + e.getMessage());
        }
    }

    @GetMapping("/exercises/summary")
    public Result<Map<String, Object>> getUserExerciseSummary(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuth(authentication);
            Map<String, Object> summary = exerciseService.getUserExerciseSummary(userId);

            return Result.success(summary);

        } catch (Exception e) {
            log.error("Failed to get exercise summary", e);
            return Result.error("Failed to get summary: " + e.getMessage());
        }
    }

    // ================================================================
    // Achievement Endpoints
    // ================================================================

    @GetMapping("/achievements")
    public Result<List<Achievement>> listAchievements(
            @RequestParam(required = false) String category) {

        try {
            List<Achievement> achievements = achievementService.getAllAchievements(category);
            return Result.success(achievements);

        } catch (Exception e) {
            log.error("Failed to list achievements", e);
            return Result.error("Failed to list achievements: " + e.getMessage());
        }
    }

    @GetMapping("/achievements/user")
    public Result<List<Map<String, Object>>> getUserAchievements(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuth(authentication);
            List<Map<String, Object>> achievements = achievementService.getUserAchievementsWithDetails(userId);

            return Result.success(achievements);

        } catch (Exception e) {
            log.error("Failed to get user achievements", e);
            return Result.error("Failed to get achievements: " + e.getMessage());
        }
    }

    @GetMapping("/achievements/progress")
    public Result<Map<String, Object>> getUserAchievementProgress(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuth(authentication);
            Map<String, Object> progress = achievementService.getUserAchievementProgress(userId);

            return Result.success(progress);

        } catch (Exception e) {
            log.error("Failed to get achievement progress", e);
            return Result.error("Failed to get progress: " + e.getMessage());
        }
    }

    @GetMapping("/achievements/leaderboard")
    public Result<List<Map<String, Object>>> getLeaderboard(
            @RequestParam(required = false) String achievementCode,
            @RequestParam(defaultValue = "10") int limit) {

        try {
            List<Map<String, Object>> leaderboard = achievementService.getLeaderboard(achievementCode, limit);
            return Result.success(leaderboard);

        } catch (Exception e) {
            log.error("Failed to get leaderboard", e);
            return Result.error("Failed to get leaderboard: " + e.getMessage());
        }
    }

    @GetMapping("/achievements/xp-leaderboard")
    public Result<List<Map<String, Object>>> getXpLeaderboard(
            @RequestParam(defaultValue = "10") int limit) {

        try {
            List<Map<String, Object>> leaderboard = achievementService.getXpLeaderboard(limit);
            return Result.success(leaderboard);

        } catch (Exception e) {
            log.error("Failed to get XP leaderboard", e);
            return Result.error("Failed to get XP leaderboard: " + e.getMessage());
        }
    }

    // ================================================================
    // Teaching Agent Endpoints
    // ================================================================

    @PostMapping("/explain")
    public Result<Map<String, Object>> getExplanation(
            @RequestBody ExplainRequest request,
            Authentication authentication) {

        try {
            Long userId = getUserIdFromAuth(authentication);

            // Get user's skill level for the language
            List<SkillProfile> profiles = skillAssessmentService.getUserSkillLevels(
                    userId, request.getLanguage());

            String skillLevel = profiles.isEmpty() ? "INTERMEDIATE" :
                    profiles.get(0).getSkillCategory();

            // Generate explanation based on skill level
            String explanation = generateExplanation(request, skillLevel);

            Map<String, Object> result = Map.of(
                    "explanation", explanation,
                    "skillLevel", skillLevel,
                    "language", request.getLanguage()
            );

            return Result.success(result);

        } catch (Exception e) {
            log.error("Failed to generate explanation", e);
            return Result.error("Failed to generate explanation: " + e.getMessage());
        }
    }

    @GetMapping("/issue/{issueId}/explanation")
    public Result<String> getIssueExplanation(@PathVariable Long issueId) {
        try {
            // This would fetch the issue and its teaching explanation
            // For now, return a placeholder
            return Result.success("Teaching explanation for issue " + issueId);

        } catch (Exception e) {
            log.error("Failed to get issue explanation", e);
            return Result.error("Failed to get explanation: " + e.getMessage());
        }
    }

    // ================================================================
    // Helper Methods
    // ================================================================

    private Long getUserIdFromAuth(Authentication authentication) {
        // Extract user ID from JWT authentication
        // Principal is now CustomUserDetails with userId included
        Object principal = authentication.getPrincipal();

        // Handle CustomUserDetails (our implementation)
        if (principal instanceof com.codereview.ai.security.CustomUserDetails) {
            return ((com.codereview.ai.security.CustomUserDetails) principal).getId();
        }

        // Handle String username (for simple tokens)
        if (principal instanceof String) {
            String username = (String) principal;
            return userService.findByUsernameWithRoles(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username))
                    .getId();
        }

        // Handle Spring Security User
        if (principal instanceof org.springframework.security.core.userdetails.User) {
            String username = ((org.springframework.security.core.userdetails.User) principal).getUsername();
            return userService.findByUsernameWithRoles(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username))
                    .getId();
        }

        throw new IllegalArgumentException("Unsupported principal type: " + principal.getClass());
    }

    private String generateExplanation(ExplainRequest request, String skillLevel) {
        // Generate skill-appropriate explanation
        String intro = switch (skillLevel) {
            case "BEGINNER" -> "Let me explain this in simple terms:\n\n";
            case "INTERMEDIATE" -> "Here's what's happening:\n\n";
            case "ADVANCED" -> "Here's a detailed analysis:\n\n";
            default -> "Here's the explanation:\n\n";
        };

        return intro + "The code you're looking at " + request.getTopic() +
                ". " + request.getContext() + "\n\n" +
                "Key points to understand:\n" +
                "1. The main purpose of this code is to " + request.getTopic() + "\n" +
                "2. It uses " + request.getLanguage() + " syntax\n" +
                "3. Consider the best practices for " + request.getCategory() + "\n";
    }

    // ================================================================
    // Request DTOs
    // ================================================================

    // Commented out due to missing LearningContent entity
    /*
    public static class CreateContentRequest {
        private String title;
        private String description;
        private LearningContent.ContentType contentType;
        private LearningContent.DifficultyLevel difficultyLevel;
        private String content;
        private String language;
        private String category;
        private List<String> tags;
        private List<String> codeExamples;
        private List<Long> prerequisites;
        private Integer estimatedMinutes;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public LearningContent.ContentType getContentType() { return contentType; }
        public void setContentType(LearningContent.ContentType contentType) { this.contentType = contentType; }
        public LearningContent.DifficultyLevel getDifficultyLevel() { return difficultyLevel; }
        public void setDifficultyLevel(LearningContent.DifficultyLevel difficultyLevel) { this.difficultyLevel = difficultyLevel; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public List<String> getCodeExamples() { return codeExamples; }
        public void setCodeExamples(List<String> codeExamples) { this.codeExamples = codeExamples; }
        public List<Long> getPrerequisites() { return prerequisites; }
        public void setPrerequisites(List<Long> prerequisites) { this.prerequisites = prerequisites; }
        public Integer getEstimatedMinutes() { return estimatedMinutes; }
        public void setEstimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
    }
    */

    public static class UpdateProgressRequest {
        private int percent;
        private int currentSection;

        public int getPercent() { return percent; }
        public void setPercent(int percent) { this.percent = percent; }
        public int getCurrentSection() { return currentSection; }
        public void setCurrentSection(int currentSection) { this.currentSection = currentSection; }
    }

    public static class CompleteContentRequest {
        private Integer score;

        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
    }

    public static class SubmitAttemptRequest {
        private String code;
        private int timeSpentSeconds;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public int getTimeSpentSeconds() { return timeSpentSeconds; }
        public void setTimeSpentSeconds(int timeSpentSeconds) { this.timeSpentSeconds = timeSpentSeconds; }
    }

    public static class ExplainRequest {
        private String topic;
        private String context;
        private String language;
        private String category;
        private String preferredDepth;

        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
        public String getContext() { return context; }
        public void setContext(String context) { this.context = context; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getPreferredDepth() { return preferredDepth; }
        public void setPreferredDepth(String preferredDepth) { this.preferredDepth = preferredDepth; }
    }
}
