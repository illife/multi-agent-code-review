package com.codereview.ai.domain.service.impl;

import com.codereview.ai.domain.model.UserProgress;
import com.codereview.ai.domain.repository.UserProgressRepository;
import com.codereview.ai.domain.service.UserProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User Progress Service Implementation
 *
 * @author Code Review AI Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProgressServiceImpl implements UserProgressService {

    private final UserProgressRepository progressRepository;

    @Override
    @Transactional
    public UserProgress startContent(Long userId, Long learningPathId) {
        UserProgress progress = progressRepository.findByUserIdAndLearningPathId(userId, learningPathId)
                .orElse(UserProgress.builder()
                        .userId(userId)
                        .learningPathId(learningPathId)
                        .status(UserProgress.ProgressStatus.NOT_STARTED)
                        .progressPercent(0)
                        .currentSection(1)
                        .timeSpentMinutes(0)
                        .build());

        if (progress.getStatus() == UserProgress.ProgressStatus.NOT_STARTED) {
            progress.setStatus(UserProgress.ProgressStatus.IN_PROGRESS);
            progress.setStartedAt(LocalDateTime.now());
        }

        progress.setLastAccessedAt(LocalDateTime.now());
        UserProgress saved = progressRepository.save(progress);

        log.info("Started learning path: userId={}, learningPathId={}", userId, learningPathId);
        return saved;
    }

    @Override
    @Transactional
    public UserProgress updateProgress(Long userId, Long learningPathId, int percent, int currentSection) {
        UserProgress progress = progressRepository.findByUserIdAndLearningPathId(userId, learningPathId)
                .orElse(startContent(userId, learningPathId));

        progress.setProgressPercent(Math.max(0, Math.min(100, percent)));
        progress.setCurrentSection(currentSection);
        progress.setLastAccessedAt(LocalDateTime.now());

        if (percent >= 100) {
            progress.setStatus(UserProgress.ProgressStatus.COMPLETED);
            progress.setCompletedAt(LocalDateTime.now());
        }

        UserProgress saved = progressRepository.save(progress);
        log.info("Updated progress: userId={}, learningPathId={}, percent={}", userId, learningPathId, percent);

        return saved;
    }

    @Override
    @Transactional
    public UserProgress completeContent(Long userId, Long learningPathId, Integer score) {
        UserProgress progress = progressRepository.findByUserIdAndLearningPathId(userId, learningPathId)
                .orElseThrow(() -> new IllegalArgumentException("Progress not found for learning path: " + learningPathId));

        progress.setStatus(UserProgress.ProgressStatus.COMPLETED);
        progress.setProgressPercent(100);
        progress.setCompletedAt(LocalDateTime.now());
        progress.setLastAccessedAt(LocalDateTime.now());

        if (score != null) {
            progress.setScore(score);
        }

        UserProgress saved = progressRepository.save(progress);
        log.info("Completed learning path: userId={}, learningPathId={}, score={}", userId, learningPathId, score);

        return saved;
    }

    @Override
    public Map<String, Object> getUserOverallStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        long completedCount = progressRepository.countCompletedByUserId(userId);
        Long totalMinutes = progressRepository.totalTimeSpentByUserId(userId);
        Double avgProgress = progressRepository.averageProgressByUserId(userId);

        stats.put("completedLearningPaths", completedCount);
        stats.put("totalTimeSpentMinutes", totalMinutes != null ? totalMinutes : 0);
        stats.put("averageProgressPercent", avgProgress != null ? avgProgress : 0.0);

        // Language-based stats commented out due to missing LearningContent entity
        // List<Object[]> byLanguage = progressRepository.countCompletedByLanguage(userId);
        // Map<String, Long> byLanguageMap = new HashMap<>();
        // for (Object[] row : byLanguage) {
        //     byLanguageMap.put((String) row[0], (Long) row[1]);
        // }
        // stats.put("completedByLanguage", byLanguageMap);

        return stats;
    }

    @Override
    public UserProgress getContentProgress(Long userId, Long learningPathId) {
        return progressRepository.findByUserIdAndLearningPathId(userId, learningPathId).orElse(null);
    }

    @Override
    public Map<String, Object> getInProgressContent(Long userId) {
        List<UserProgress> inProgress = progressRepository.findInProgressContent(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("count", inProgress.size());
        result.put("items", inProgress);

        return result;
    }

    @Override
    public Map<String, Object> getCompletedContent(Long userId) {
        List<UserProgress> completed = progressRepository.findByUserIdAndStatusOrderByLastAccessed(
                userId, UserProgress.ProgressStatus.COMPLETED);

        Map<String, Object> result = new HashMap<>();
        result.put("count", completed.size());
        result.put("items", completed);

        return result;
    }

    @Override
    @Transactional
    public UserProgress addTimeSpent(Long userId, Long learningPathId, int minutes) {
        UserProgress progress = progressRepository.findByUserIdAndLearningPathId(userId, learningPathId)
                .orElseThrow(() -> new IllegalArgumentException("Progress not found"));

        int currentMinutes = progress.getTimeSpentMinutes() != null ? progress.getTimeSpentMinutes() : 0;
        progress.setTimeSpentMinutes(currentMinutes + minutes);
        progress.setLastAccessedAt(LocalDateTime.now());

        return progressRepository.save(progress);
    }

    @Override
    @Transactional
    public UserProgress resetProgress(Long userId, Long learningPathId) {
        UserProgress progress = progressRepository.findByUserIdAndLearningPathId(userId, learningPathId)
                .orElseThrow(() -> new IllegalArgumentException("Progress not found"));

        progress.setStatus(UserProgress.ProgressStatus.NOT_STARTED);
        progress.setProgressPercent(0);
        progress.setCurrentSection(1);
        progress.setScore(null);
        progress.setStartedAt(null);
        progress.setCompletedAt(null);
        progress.setLastAccessedAt(LocalDateTime.now());

        UserProgress saved = progressRepository.save(progress);
        log.info("Reset progress: userId={}, learningPathId={}", userId, learningPathId);

        return saved;
    }

    @Override
    @Transactional
    public void deleteProgress(Long userId, Long learningPathId) {
        UserProgress progress = progressRepository.findByUserIdAndLearningPathId(userId, learningPathId)
                .orElseThrow(() -> new IllegalArgumentException("Progress not found"));

        progressRepository.delete(progress);
        log.info("Deleted progress: userId={}, learningPathId={}", userId, learningPathId);
    }

    @Override
    public Map<String, Object> getRecentlyAccessed(Long userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<UserProgress> recent = progressRepository.findRecentlyAccessed(userId, since);

        Map<String, Object> result = new HashMap<>();
        result.put("count", recent.size());
        result.put("items", recent);

        return result;
    }
}
