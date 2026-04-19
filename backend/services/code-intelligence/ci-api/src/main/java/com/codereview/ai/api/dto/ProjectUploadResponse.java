package com.codereview.ai.api.dto;

import com.codereview.ai.domain.model.Project;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Project Upload Response DTO
 * Wrapper for the service layer response
 *
 * @author Code Review AI Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectUploadResponse {
    private Long projectId;
    private String projectName;
    private Project.ProjectStatus status;
    private String message;
    private Project.UploadType uploadType;
    private Integer totalFiles;
}
