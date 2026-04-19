package com.codereview.ai.api.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Update Profile Request DTO
 *
 * @author Code Review AI Team
 */
@Data
public class UpdateProfileRequest {

    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    @Size(max = 100, message = "Department must not exceed 100 characters")
    private String department;
}
