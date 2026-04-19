package com.codereview.ai.domain.repository;

import com.codereview.ai.domain.model.SkillProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Skill Profile Repository
 *
 * @author Code Review AI Team
 */
@Repository
public interface SkillProfileRepository extends JpaRepository<SkillProfile, Long> {

    Optional<SkillProfile> findByUserIdAndLanguageAndCategory(Long userId, String language, String category);

    List<SkillProfile> findByUserId(Long userId);

    List<SkillProfile> findByUserIdAndLanguage(Long userId, String language);

    Page<SkillProfile> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT sp FROM SkillProfile sp WHERE sp.userId = :userId " +
           "ORDER BY sp.skillLevel DESC")
    List<SkillProfile> findByUserIdOrderBySkillLevelDesc(@Param("userId") Long userId);

    @Query("SELECT sp FROM SkillProfile sp WHERE sp.userId = :userId " +
           "AND sp.language = :language ORDER BY sp.skillLevel DESC")
    List<SkillProfile> findByUserIdAndLanguageOrderBySkillLevelDesc(
            @Param("userId") Long userId,
            @Param("language") String language);

    @Query("SELECT AVG(sp.skillLevel) FROM SkillProfile sp WHERE sp.userId = :userId")
    Double getAverageSkillLevel(@Param("userId") Long userId);

    @Query("SELECT SUM(sp.totalXp) FROM SkillProfile sp WHERE sp.userId = :userId")
    Long getTotalXp(@Param("userId") Long userId);

    @Query("SELECT sp FROM SkillProfile sp WHERE sp.skillLevel >= :minLevel " +
           "ORDER BY sp.skillLevel DESC, sp.totalXp DESC")
    List<SkillProfile> findTopBySkillLevel(@Param("minLevel") int minLevel, Pageable pageable);

    @Query("SELECT sp.language, sp.category, sp.skillLevel FROM SkillProfile sp " +
           "WHERE sp.userId = :userId ORDER BY sp.skillLevel DESC")
    List<Object[]> getUserSkillSummary(@Param("userId") Long userId);

    @Query("SELECT sp FROM SkillProfile sp WHERE sp.userId = :userId " +
           "AND sp.language = :language AND sp.category = :category")
    Optional<SkillProfile> findUserSkillForLanguageAndCategory(
            @Param("userId") Long userId,
            @Param("language") String language,
            @Param("category") String category);

    boolean existsByUserIdAndLanguageAndCategory(Long userId, String language, String category);
}
