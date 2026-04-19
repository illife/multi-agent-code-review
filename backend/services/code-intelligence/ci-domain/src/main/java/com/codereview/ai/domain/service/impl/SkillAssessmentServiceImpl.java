package com.codereview.ai.domain.service.impl;

import com.codereview.ai.domain.model.CodeIssue;
import com.codereview.ai.domain.model.CodeReview;
import com.codereview.ai.domain.model.SkillProfile;
import com.codereview.ai.domain.repository.CodeIssueRepository;
import com.codereview.ai.domain.repository.SkillProfileRepository;
import com.codereview.ai.domain.service.SkillAssessmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Skill Assessment Service Implementation
 *
 * @author Code Review AI Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillAssessmentServiceImpl implements SkillAssessmentService {

    private final SkillProfileRepository skillProfileRepository;
    private final CodeIssueRepository issueRepository;

    private static final Map<String, List<String>> DEFAULT_CATEGORIES;
    static {
        Map<String, List<String>> map = new HashMap<>();
        map.put("security", Arrays.asList("security", "authentication", "authorization"));
        map.put("performance", Arrays.asList("performance", "optimization", "efficiency"));
        map.put("best-practices", Arrays.asList("best-practices", "code-quality", "maintainability"));
        DEFAULT_CATEGORIES = Collections.unmodifiableMap(map);
    }

    @Override
    @Transactional
    public SkillProfile getSkillProfile(Long userId, String language, String category) {
        return skillProfileRepository
                .findByUserIdAndLanguageAndCategory(userId, language, category)
                .orElseGet(() -> createUserSkillProfile(userId, language, category));
    }

    @Override
    public List<SkillProfile> getUserSkillLevels(Long userId, String language) {
        List<SkillProfile> profiles = skillProfileRepository.findByUserIdAndLanguageOrderBySkillLevelDesc(userId, language);

        if (profiles.isEmpty()) {
            // Initialize with default categories
            initializeUserSkills(userId, List.of(language));
            return skillProfileRepository.findByUserIdAndLanguageOrderBySkillLevelDesc(userId, language);
        }

        return profiles;
    }

    @Override
    public List<SkillProfile> getAllUserSkills(Long userId) {
        return skillProfileRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public List<SkillProfile> assessFromReview(Long userId, CodeReview review) {
        List<SkillProfile> updatedProfiles = new ArrayList<>();
        String language = review.getLanguage();

        // Get issues by category to update relevant skills
        Map<String, List<CodeIssue>> issuesByCategory = new HashMap<>();

        List<CodeIssue> issues = issueRepository.findByReviewIdOrderBySeverity(review.getId());
        for (CodeIssue issue : issues) {
            String category = issue.getCategory() != null ? issue.getCategory() : "best-practices";
            issuesByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(issue);
        }

        // Update skill profiles based on issues found
        for (Map.Entry<String, List<CodeIssue>> entry : issuesByCategory.entrySet()) {
            String category = entry.getKey();
            int issueCount = entry.getValue().size();

            SkillProfile profile = getSkillProfile(userId, language, category);

            // Increment reviews completed
            profile.setReviewsCompleted(profile.getReviewsCompleted() + 1);

            // Add XP based on review (base XP + bonus for finding issues)
            int xpEarned = 5 + (issueCount * 2);
            profile.addXp(xpEarned);
            profile.setLastAssessedAt(LocalDateTime.now());

            updatedProfiles.add(skillProfileRepository.save(profile));
        }

        // Also update general best-practices profile
        SkillProfile generalProfile = getSkillProfile(userId, language, "best-practices");
        generalProfile.setReviewsCompleted(generalProfile.getReviewsCompleted() + 1);
        generalProfile.addXp(5);
        generalProfile.setLastAssessedAt(LocalDateTime.now());
        updatedProfiles.add(skillProfileRepository.save(generalProfile));

        log.info("Assessed skills from review: userId={}, language={}, updatedProfiles={}",
                userId, language, updatedProfiles.size());

        return updatedProfiles;
    }

    @Override
    @Transactional
    public SkillProfile assessFromExercise(Long userId, String language, String category, boolean passed) {
        SkillProfile profile = getSkillProfile(userId, language, category);

        profile.setExercisesCompleted(profile.getExercisesCompleted() + 1);

        if (passed) {
            // Award more XP for passing exercises
            int xpEarned = 15;
            profile.addXp(xpEarned);
        } else {
            // Small XP for attempt
            profile.addXp(3);
        }

        profile.setLastAssessedAt(LocalDateTime.now());

        SkillProfile saved = skillProfileRepository.save(profile);
        log.info("Assessed skills from exercise: userId={}, language={}, category={}, passed={}",
                userId, language, category, passed);

        return saved;
    }

    @Override
    public Map<String, Object> getImprovementResources(Long userId, String language) {
        Map<String, Object> resources = new HashMap<>();

        List<SkillProfile> profiles = getUserSkillLevels(userId, language);

        // Find weakest skills
        List<Map<String, Object>> weakSkills = profiles.stream()
                .filter(p -> p.getSkillLevel() < 50)
                .sorted(Comparator.comparingInt(SkillProfile::getSkillLevel))
                .limit(3)
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("category", p.getCategory());
                    map.put("skillLevel", p.getSkillLevel());
                    map.put("recommendedAction", getRecommendationForSkill(p.getCategory(), p.getSkillLevel()));
                    return map;
                })
                .collect(Collectors.toList());

        resources.put("weakSkills", weakSkills);
        resources.put("language", language);
        resources.put("overallLevel", getOverallSkillLevel(profiles));

        return resources;
    }

    @Override
    @Transactional
    public SkillProfile addXp(Long userId, String language, String category, int xp) {
        SkillProfile profile = getSkillProfile(userId, language, category);
        profile.addXp(xp);
        profile.setLastAssessedAt(LocalDateTime.now());
        return skillProfileRepository.save(profile);
    }

    @Override
    public Map<String, Object> getUserSkillSummary(Long userId) {
        Map<String, Object> summary = new HashMap<>();

        List<SkillProfile> profiles = getAllUserSkills(userId);

        Double avgLevel = skillProfileRepository.getAverageSkillLevel(userId);
        Long totalXp = skillProfileRepository.getTotalXp(userId);

        summary.put("totalSkills", profiles.size());
        summary.put("averageLevel", avgLevel != null ? avgLevel : 0);
        summary.put("totalXp", totalXp != null ? totalXp : 0);

        // Group by language
        Map<String, List<SkillProfile>> byLanguage = profiles.stream()
                .collect(Collectors.groupingBy(SkillProfile::getLanguage));

        Map<String, Object> languageSummary = new HashMap<>();
        for (Map.Entry<String, List<SkillProfile>> entry : byLanguage.entrySet()) {
            double langAvg = entry.getValue().stream()
                    .mapToInt(SkillProfile::getSkillLevel)
                    .average()
                    .orElse(0);
            languageSummary.put(entry.getKey(), Map.of(
                    "averageLevel", (int) langAvg,
                    "categories", entry.getValue().size()
            ));
        }
        summary.put("byLanguage", languageSummary);

        return summary;
    }

    @Override
    public List<Map<String, Object>> getLeaderboard(String language, String category, int limit) {
        List<SkillProfile> profiles = skillProfileRepository
                .findUserSkillForLanguageAndCategory(null, language, category)
                .map(List::of)
                .orElseGet(() -> {
                    // Get all profiles for this language/category combination
                    return skillProfileRepository.findByUserId(0L).stream()
                            .filter(p -> p.getLanguage().equals(language) && p.getCategory().equals(category))
                            .sorted(Comparator.comparingInt(SkillProfile::getSkillLevel).reversed())
                            .limit(limit)
                            .toList();
                });

        return profiles.stream()
                .sorted(Comparator.comparingInt(SkillProfile::getSkillLevel).reversed())
                .limit(limit)
                .map(p -> Map.<String, Object>of(
                        "userId", p.getUserId(),
                        "skillLevel", p.getSkillLevel(),
                        "totalXp", p.getTotalXp()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public String getSkillCategory(Long userId, String language, String category) {
        SkillProfile profile = getSkillProfile(userId, language, category);
        return profile.getSkillCategory();
    }

    @Override
    @Transactional
    public void initializeUserSkills(Long userId, List<String> languages) {
        for (String language : languages) {
            for (Map.Entry<String, List<String>> entry : DEFAULT_CATEGORIES.entrySet()) {
                String mainCategory = entry.getKey();
                for (String subCategory : entry.getValue()) {
                    if (!skillProfileRepository.existsByUserIdAndLanguageAndCategory(userId, language, subCategory)) {
                        SkillProfile profile = SkillProfile.builder()
                                .userId(userId)
                                .language(language)
                                .category(subCategory)
                                .skillLevel(0)
                                .exercisesCompleted(0)
                                .reviewsCompleted(0)
                                .lessonsCompleted(0)
                                .totalXp(0)
                                .build();
                        skillProfileRepository.save(profile);
                    }
                }
            }
        }

        log.info("Initialized skills for user: userId={}, languages={}", userId, languages);
    }

    @Override
    public Map<String, Object> getRecommendedContentForSkillGaps(Long userId, String language) {
        List<SkillProfile> profiles = getUserSkillLevels(userId, language);

        // Find categories with low skill levels
        List<String> weakCategories = profiles.stream()
                .filter(p -> p.getSkillLevel() < 40)
                .map(SkillProfile::getCategory)
                .limit(3)
                .toList();

        return Map.of(
                "userId", userId,
                "language", language,
                "weakCategories", weakCategories,
                "recommendations", weakCategories.stream()
                        .map(cat -> Map.of(
                                "category", cat,
                                "suggestedAction", getRecommendationForSkill(cat, 30)
                        ))
                        .toList()
        );
    }

    /**
     * Create a new user skill profile
     */
    private SkillProfile createUserSkillProfile(Long userId, String language, String category) {
        SkillProfile profile = SkillProfile.builder()
                .userId(userId)
                .language(language)
                .category(category)
                .skillLevel(0)
                .exercisesCompleted(0)
                .reviewsCompleted(0)
                .lessonsCompleted(0)
                .totalXp(0)
                .build();

        return skillProfileRepository.save(profile);
    }

    /**
     * Get overall skill level from profiles
     */
    private String getOverallSkillLevel(List<SkillProfile> profiles) {
        if (profiles.isEmpty()) return "BEGINNER";

        double avg = profiles.stream().mapToInt(SkillProfile::getSkillLevel).average().orElse(0);
        if (avg >= 70) return "ADVANCED";
        if (avg >= 40) return "INTERMEDIATE";
        return "BEGINNER";
    }

    /**
     * Get recommendation for skill improvement
     */
    private String getRecommendationForSkill(String category, int skillLevel) {
        return switch (category) {
            case "security" -> "Complete security-focused lessons and exercises";
            case "performance" -> "Practice optimization techniques and profiling";
            case "best-practices" -> "Review style guides and refactoring patterns";
            case "code-quality" -> "Focus on clean code principles and SOLID patterns";
            case "authentication" -> "Study authentication protocols and authorization patterns";
            default -> "Practice more exercises in this category";
        };
    }
}
