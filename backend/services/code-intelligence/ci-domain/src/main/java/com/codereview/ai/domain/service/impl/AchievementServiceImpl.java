package com.codereview.ai.domain.service.impl;

import com.codereview.ai.domain.model.Achievement;
import com.codereview.ai.domain.model.SkillProfile;
import com.codereview.ai.domain.model.UserAchievement;
import com.codereview.ai.domain.repository.AchievementRepository;
import com.codereview.ai.domain.repository.SkillProfileRepository;
import com.codereview.ai.domain.repository.UserAchievementRepository;
import com.codereview.ai.domain.service.AchievementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Achievement Service Implementation
 *
 * @author Code Review AI Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final SkillProfileRepository skillProfileRepository;

    @Override
    @Transactional
    public UserAchievement unlockAchievement(Long userId, Long achievementId) {
        // Check if already unlocked
        Optional<UserAchievement> existing = userAchievementRepository
                .findByUserIdAndAchievementId(userId, achievementId);

        if (existing.isPresent()) {
            return existing.get();
        }

        Achievement achievement = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new IllegalArgumentException("Achievement not found: " + achievementId));

        UserAchievement userAchievement = UserAchievement.builder()
                .userId(userId)
                .achievementId(achievementId)
                .earnedAt(LocalDateTime.now())
                .build();

        UserAchievement saved = userAchievementRepository.save(userAchievement);
        log.info("Unlocked achievement: userId={}, achievementId={}, code={}",
                userId, achievementId, achievement.getCode());

        return saved;
    }

    @Override
    @Transactional
    public UserAchievement unlockAchievementByCode(Long userId, String achievementCode) {
        Achievement achievement = achievementRepository.findByCode(achievementCode)
                .orElseThrow(() -> new IllegalArgumentException("Achievement not found: " + achievementCode));

        return unlockAchievement(userId, achievement.getId());
    }

    @Override
    public List<UserAchievement> getUserAchievements(Long userId) {
        return userAchievementRepository.findByUserIdWithAchievementOrderByEarnedAtDesc(userId);
    }

    @Override
    public List<Map<String, Object>> getUserAchievementsWithDetails(Long userId) {
        List<UserAchievement> userAchievements = getUserAchievements(userId);

        return userAchievements.stream()
                .map(ua -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("userAchievement", ua);
                    result.put("achievement", ua.getAchievement());
                    return result;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Achievement> getAllAchievements(String category) {
        if (category != null) {
            return achievementRepository.findActiveByCategoryOrderByXp(category);
        }
        return achievementRepository.findActiveAchievementsOrderedByXp();
    }

    @Override
    @Transactional
    public List<UserAchievement> checkAndUnlockAchievements(Long userId, String eventType, Map<String, Object> eventData) {
        List<UserAchievement> newlyUnlocked = new ArrayList<>();
        List<Achievement> allAchievements = achievementRepository.findActiveAchievementsGroupedByCategory();

        for (Achievement achievement : allAchievements) {
            // Check if already unlocked
            if (userAchievementRepository.findByUserIdAndAchievementId(userId, achievement.getId()).isPresent()) {
                continue;
            }

            // Check if this achievement matches the event type
            if (shouldCheckAchievement(achievement, eventType)) {
                if (evaluateAchievementRequirement(userId, achievement, eventData)) {
                    UserAchievement unlocked = unlockAchievement(userId, achievement.getId());
                    newlyUnlocked.add(unlocked);
                }
            }
        }

        if (!newlyUnlocked.isEmpty()) {
            log.info("Unlocked {} achievements for userId={}, eventType={}",
                    newlyUnlocked.size(), userId, eventType);
        }

        return newlyUnlocked;
    }

    @Override
    public List<Map<String, Object>> getLeaderboard(String achievementCode, int limit) {
        List<Object[]> leaderboard;

        if (achievementCode != null) {
            Achievement achievement = achievementRepository.findByCode(achievementCode)
                    .orElseThrow(() -> new IllegalArgumentException("Achievement not found: " + achievementCode));

            // Get users who have this specific achievement
            leaderboard = userAchievementRepository.findAll().stream()
                    .filter(ua -> ua.getAchievementId().equals(achievement.getId()))
                    .collect(Collectors.groupingBy(UserAchievement::getUserId, Collectors.counting()))
                    .entrySet().stream()
                    .map(e -> new Object[]{e.getKey(), e.getValue()})
                    .sorted((a, b) -> Long.compare((Long) b[1], (Long) a[1]))
                    .limit(limit)
                    .toList();
        } else {
            // General achievement count leaderboard
            leaderboard = userAchievementRepository.findLeaderboard(PageRequest.of(0, limit));
        }

        return leaderboard.stream()
                .map(row -> Map.<String, Object>of(
                        "userId", row[0],
                        "count", row[1]
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getXpLeaderboard(int limit) {
        List<Object[]> leaderboard = userAchievementRepository.findXpLeaderboard(PageRequest.of(0, limit));

        return leaderboard.stream()
                .map(row -> Map.<String, Object>of(
                        "userId", row[0],
                        "totalXp", row[1]
                ))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getUserAchievementProgress(Long userId) {
        Map<String, Object> progress = new HashMap<>();

        long totalEarned = userAchievementRepository.countByUserId(userId);
        Long totalXp = userAchievementRepository.getTotalXpByUserId(userId);
        List<Object[]> byCategory = userAchievementRepository.countByCategoryForUser(userId);

        progress.put("totalEarned", totalEarned);
        progress.put("totalXp", totalXp != null ? totalXp : 0);

        Map<String, Long> byCategoryMap = new HashMap<>();
        for (Object[] row : byCategory) {
            byCategoryMap.put((String) row[0], (Long) row[1]);
        }
        progress.put("byCategory", byCategoryMap);

        return progress;
    }

    @Override
    public Long getUserTotalXp(Long userId) {
        Long totalXp = userAchievementRepository.getTotalXpByUserId(userId);
        return totalXp != null ? totalXp : 0L;
    }

    @Override
    public List<Map<String, Object>> getRecentAchievements(Long userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<UserAchievement> recent = userAchievementRepository.findRecentlyEarned(userId, since);

        return recent.stream()
                .map(ua -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("userAchievement", ua);
                    result.put("achievement", ua.getAchievement());
                    return result;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Achievement getAchievementByCode(String code) {
        return achievementRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Achievement not found: " + code));
    }

    @Override
    @Transactional
    public Achievement createAchievement(Achievement achievement) {
        Achievement saved = achievementRepository.save(achievement);
        log.info("Created achievement: code={}, title={}", saved.getCode(), saved.getTitle());
        return saved;
    }

    @Override
    @Transactional
    public Achievement updateAchievement(Long achievementId, Achievement achievement) {
        Achievement existing = achievementRepository.findById(achievementId)
                .orElseThrow(() -> new IllegalArgumentException("Achievement not found: " + achievementId));

        existing.setTitle(achievement.getTitle());
        existing.setDescription(achievement.getDescription());
        existing.setIconUrl(achievement.getIconUrl());
        existing.setCategory(achievement.getCategory());
        existing.setRequirements(achievement.getRequirements());
        existing.setXpReward(achievement.getXpReward());
        existing.setBadgeColor(achievement.getBadgeColor());
        existing.setIsActive(achievement.getIsActive());

        return achievementRepository.save(existing);
    }

    @Override
    public boolean hasAchievement(Long userId, String achievementCode) {
        return achievementRepository.findByCode(achievementCode)
                .map(achievement -> userAchievementRepository
                        .findByUserIdAndAchievementId(userId, achievement.getId())
                        .isPresent())
                .orElse(false);
    }

    @Override
    public Map<String, Object> getAchievementCategories() {
        List<Object[]> summary = achievementRepository.getXpSummaryByCategory();

        Map<String, Object> result = new HashMap<>();
        result.put("categories", summary);

        long totalAchievements = achievementRepository.countByIsActive(true);
        result.put("totalActive", totalAchievements);

        return result;
    }

    /**
     * Check if achievement should be evaluated for the event type
     */
    private boolean shouldCheckAchievement(Achievement achievement, String eventType) {
        String requirementType = achievement.getRequirementAsString("type");

        if (requirementType == null) {
            return false;
        }

        return switch (requirementType) {
            case "count" -> eventType.contains("completed") || eventType.contains("passed");
            case "skill_level" -> eventType.contains("skill") || eventType.contains("level");
            case "streak" -> eventType.contains("streak") || eventType.contains("daily");
            default -> false;
        };
    }

    /**
     * Evaluate achievement requirement against event data
     */
    private boolean evaluateAchievementRequirement(Long userId, Achievement achievement, Map<String, Object> eventData) {
        String requirementType = achievement.getRequirementAsString("type");

        if (requirementType == null) {
            return false;
        }

        return switch (requirementType) {
            case "count" -> evaluateCountAchievement(userId, achievement, eventData);
            case "skill_level" -> evaluateSkillLevelAchievement(userId, achievement);
            case "streak" -> evaluateStreakAchievement(userId, achievement, eventData);
            default -> false;
        };
    }

    /**
     * Evaluate count-based achievement
     */
    private boolean evaluateCountAchievement(Long userId, Achievement achievement, Map<String, Object> eventData) {
        String field = achievement.getRequirementAsString("field");
        Integer target = achievement.getRequirementAsInt("target");

        if (field == null || target == null) {
            return false;
        }

        Long currentCount = switch (field) {
            case "reviews_completed" -> 0L; // Would query review count
            case "lessons_completed" -> 0L; // Would query lesson count
            case "exercises_completed" -> 0L; // Would query exercise count
            case "bugs_fixed" -> 0L;
            default -> 0L;
        };

        // Also check event data for current count
        Object eventCount = eventData.get("count");
        if (eventCount instanceof Number) {
            currentCount = ((Number) eventCount).longValue();
        }

        return currentCount >= target;
    }

    /**
     * Evaluate skill level achievement
     */
    private boolean evaluateSkillLevelAchievement(Long userId, Achievement achievement) {
        Integer targetLevel = achievement.getRequirementAsInt("target");

        if (targetLevel == null) {
            return false;
        }

        // Check if user has any skill profile at or above target level
        List<SkillProfile> profiles = skillProfileRepository.findByUserId(userId);

        return profiles.stream().anyMatch(p -> p.getSkillLevel() >= targetLevel);
    }

    /**
     * Evaluate streak achievement
     */
    private boolean evaluateStreakAchievement(Long userId, Achievement achievement, Map<String, Object> eventData) {
        Integer targetStreak = achievement.getRequirementAsInt("target");

        if (targetStreak == null) {
            return false;
        }

        Object currentStreak = eventData.get("streak");
        if (currentStreak instanceof Number) {
            return ((Number) currentStreak).intValue() >= targetStreak;
        }

        return false;
    }
}
