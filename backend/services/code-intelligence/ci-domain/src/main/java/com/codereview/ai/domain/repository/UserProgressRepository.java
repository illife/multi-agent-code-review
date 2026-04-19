package com.codereview.ai.domain.repository;

import com.codereview.ai.domain.model.UserProgress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * User Progress Repository
 *
 * @author Code Review AI Team
 */
@Repository
public interface UserProgressRepository extends JpaRepository<UserProgress, Long> {

    Optional<UserProgress> findByUserIdAndLearningPathId(Long userId, Long learningPathId);

    Page<UserProgress> findByUserId(Long userId, Pageable pageable);

    Page<UserProgress> findByUserIdAndStatus(Long userId, UserProgress.ProgressStatus status, Pageable pageable);

    @Query("SELECT up FROM UserProgress up WHERE up.userId = :userId AND up.status = :status " +
           "ORDER BY up.lastAccessedAt DESC")
    List<UserProgress> findByUserIdAndStatusOrderByLastAccessed(
            @Param("userId") Long userId,
            @Param("status") UserProgress.ProgressStatus status);

    @Query("SELECT up FROM UserProgress up WHERE up.userId = :userId AND up.status = 'IN_PROGRESS' " +
           "ORDER BY up.lastAccessedAt DESC")
    List<UserProgress> findInProgressContent(@Param("userId") Long userId);

    @Query("SELECT COUNT(up) FROM UserProgress up WHERE up.userId = :userId AND up.status = 'COMPLETED'")
    long countCompletedByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(up.timeSpentMinutes) FROM UserProgress up WHERE up.userId = :userId")
    Long totalTimeSpentByUserId(@Param("userId") Long userId);

    @Query("SELECT AVG(up.progressPercent) FROM UserProgress up WHERE up.userId = :userId")
    Double averageProgressByUserId(@Param("userId") Long userId);

    @Query("SELECT up FROM UserProgress up WHERE up.userId = :userId " +
           "AND up.lastAccessedAt >= :since ORDER BY up.lastAccessedAt DESC")
    List<UserProgress> findRecentlyAccessed(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);

    @Query("SELECT up.learningPathId FROM UserProgress up WHERE up.userId = :userId AND up.status = 'COMPLETED'")
    List<Long> findCompletedLearningPathIdsByUserId(@Param("userId") Long userId);

    // Commented out due to missing LearningContent entity
    // @Query("SELECT lc.language, COUNT(up) FROM UserProgress up " +
    //        "JOIN LearningContent lc ON up.contentId = lc.id " +
    //        "WHERE up.userId = :userId AND up.status = 'COMPLETED' " +
    //        "GROUP BY lc.language")
    // List<Object[]> countCompletedByLanguage(@Param("userId") Long userId);

    @Query("SELECT up FROM UserProgress up WHERE up.userId = :userId")
    List<UserProgress> findAllByUserId(@Param("userId") Long userId);
}
