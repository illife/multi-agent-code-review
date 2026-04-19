package com.codereview.ai.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Skill Profile entity
 * Represents user skill levels by language and category
 *
 * @author Code Review AI Team
 */
@Entity
@Table(name = "skill_profiles", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "language", "category"})
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(length = 50, nullable = false)
    private String language;

    @Column(length = 100, nullable = false)
    private String category;

    @Column(nullable = false)
    private Integer skillLevel;

    @Column
    @Builder.Default
    private Integer exercisesCompleted = 0;

    @Column
    @Builder.Default
    private Integer reviewsCompleted = 0;

    @Column
    @Builder.Default
    private Integer lessonsCompleted = 0;

    @Column
    @Builder.Default
    private Integer totalXp = 0;

    @Column
    private LocalDateTime lastAssessedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Get skill level category
     */
    public String getSkillCategory() {
        if (skillLevel >= 90) return "EXPERT";
        if (skillLevel >= 70) return "ADVANCED";
        if (skillLevel >= 50) return "INTERMEDIATE";
        if (skillLevel >= 30) return "BEGINNER";
        return "NOVICE";
    }

    /**
     * Add XP and update skill level
     */
    public void addXp(int xp) {
        this.totalXp += xp;
        // Simple formula: each 100 XP gives approximately 10 skill level
        // Diminishing returns as skill increases
        int levelIncrease = xp / (10 + (skillLevel / 10));
        this.skillLevel = Math.min(100, this.skillLevel + levelIncrease);
    }
}
