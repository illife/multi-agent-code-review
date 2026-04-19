package com.codereview.ai.domain.repository;

import com.codereview.ai.domain.model.Achievement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Achievement Repository
 *
 * @author Code Review AI Team
 */
@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    Optional<Achievement> findByCode(String code);

    Page<Achievement> findByIsActive(Boolean isActive, Pageable pageable);

    Page<Achievement> findByCategory(String category, Pageable pageable);

    List<Achievement> findByCategoryAndIsActive(String category, Boolean isActive);

    @Query("SELECT a FROM Achievement a WHERE a.isActive = true ORDER BY a.xpReward DESC")
    List<Achievement> findActiveAchievementsOrderedByXp();

    @Query("SELECT a FROM Achievement a WHERE a.category = :category AND a.isActive = true " +
           "ORDER BY a.xpReward DESC")
    List<Achievement> findActiveByCategoryOrderByXp(@Param("category") String category);

    @Query("SELECT a FROM Achievement a WHERE a.isActive = true " +
           "ORDER BY " +
           "CASE a.category " +
           "  WHEN 'skill' THEN 1 " +
           "  WHEN 'review' THEN 2 " +
           "  WHEN 'exercise' THEN 3 " +
           "  WHEN 'learning' THEN 4 " +
           "  ELSE 5 " +
           "END, a.xpReward DESC")
    List<Achievement> findActiveAchievementsGroupedByCategory();

    long countByIsActive(Boolean isActive);

    @Query("SELECT COUNT(a) FROM Achievement a WHERE a.isActive = true AND a.category = :category")
    long countActiveByCategory(@Param("category") String category);

    @Query("SELECT a.category, SUM(a.xpReward), COUNT(a) FROM Achievement a " +
           "WHERE a.isActive = true GROUP BY a.category")
    List<Object[]> getXpSummaryByCategory();
}
