package com.codereview.ai.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Achievement entity
 * Represents gamification achievements
 *
 * @author Code Review AI Team
 */
@Entity
@Table(name = "achievements", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"code"})
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, unique = true, nullable = false)
    private String code;

    @Column(length = 255, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String iconUrl;

    @Column(length = 50)
    private String category;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSONB")
    private Map<String, Object> requirements;

    @Column
    @Builder.Default
    private Integer xpReward = 0;

    @Column(length = 20)
    @Builder.Default
    private String badgeColor = "#3B82F6";

    @Column
    @Builder.Default
    private Boolean isActive = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Achievement categories
     */
    public static final String CATEGORY_REVIEW = "review";
    public static final String CATEGORY_EXERCISE = "exercise";
    public static final String CATEGORY_LEARNING = "learning";
    public static final String CATEGORY_SKILL = "skill";
    public static final String CATEGORY_STREAK = "streak";

    /**
     * Get requirement value by key
     */
    public Object getRequirement(String key) {
        return requirements != null ? requirements.get(key) : null;
    }

    /**
     * Get requirement value as integer
     */
    public Integer getRequirementAsInt(String key) {
        Object value = getRequirement(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    /**
     * Get requirement value as string
     */
    public String getRequirementAsString(String key) {
        Object value = getRequirement(key);
        return value != null ? value.toString() : null;
    }
}
