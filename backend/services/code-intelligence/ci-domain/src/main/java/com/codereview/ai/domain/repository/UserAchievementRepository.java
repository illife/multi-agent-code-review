package com.codereview.ai.domain.repository;

import com.codereview.ai.domain.model.UserAchievement;
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
 * User Achievement Repository
 *
 * @author Code Review AI Team
 */
@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    Optional<UserAchievement> findByUserIdAndAchievementId(Long userId, Long achievementId);

    Page<UserAchievement> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT ua FROM UserAchievement ua WHERE ua.userId = :userId " +
           "ORDER BY ua.earnedAt DESC")
    List<UserAchievement> findByUserIdOrderByEarnedAtDesc(@Param("userId") Long userId);

    @Query("SELECT ua FROM UserAchievement ua JOIN FETCH ua.achievement a " +
           "WHERE ua.userId = :userId ORDER BY ua.earnedAt DESC")
    List<UserAchievement> findByUserIdWithAchievementOrderByEarnedAtDesc(@Param("userId") Long userId);

    @Query("SELECT COUNT(ua) FROM UserAchievement ua WHERE ua.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(a.xpReward) FROM UserAchievement ua " +
           "JOIN Achievement a ON ua.achievementId = a.id " +
           "WHERE ua.userId = :userId")
    Long getTotalXpByUserId(@Param("userId") Long userId);

    @Query("SELECT a.category, COUNT(ua) FROM UserAchievement ua " +
           "JOIN Achievement a ON ua.achievementId = a.id " +
           "WHERE ua.userId = :userId " +
           "GROUP BY a.category")
    List<Object[]> countByCategoryForUser(@Param("userId") Long userId);

    @Query("SELECT ua FROM UserAchievement ua WHERE ua.userId = :userId " +
           "AND ua.earnedAt >= :since ORDER BY ua.earnedAt DESC")
    List<UserAchievement> findRecentlyEarned(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);

    @Query("SELECT ua.userId, COUNT(ua) as achievementCount " +
           "FROM UserAchievement ua " +
           "GROUP BY ua.userId " +
           "ORDER BY achievementCount DESC")
    List<Object[]> findLeaderboard(Pageable pageable);

    @Query("SELECT ua.userId, SUM(a.xpReward) as totalXp " +
           "FROM UserAchievement ua " +
           "JOIN Achievement a ON ua.achievementId = a.id " +
           "GROUP BY ua.userId " +
           "ORDER BY totalXp DESC")
    List<Object[]> findXpLeaderboard(Pageable pageable);

    @Query("SELECT ua FROM UserAchievement ua " +
           "WHERE ua.userId = :userId " +
           "AND EXISTS (SELECT 1 FROM Achievement a " +
           "            WHERE a.id = ua.achievementId " +
           "            AND a.category = :category)")
    List<UserAchievement> findByUserIdAndCategory(@Param("userId") Long userId, @Param("category") String category);
}
