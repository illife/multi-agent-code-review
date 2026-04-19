package com.codereview.ai.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ProjectReport entity
 * Contains the comprehensive analysis report for a project
 *
 * @author Code Review AI Team
 */
@Entity
@Table(name = "project_reports")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProjectReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    @JsonIgnore
    private Project project;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    @Column(columnDefinition = "TEXT")
    private String metrics;

    @Column(columnDefinition = "TEXT")
    private String recommendations;

    @Column(name = "file_statistics", columnDefinition = "TEXT")
    private String fileStatistics;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Risk level enum
     * Defines the severity level of issues found in the project
     */
    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}
