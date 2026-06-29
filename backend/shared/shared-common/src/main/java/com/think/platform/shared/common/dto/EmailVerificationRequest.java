package com.think.platform.shared.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Email verification request DTO.
 */
@Data
public class EmailVerificationRequest {

    @NotBlank(message = "Token不能为空")
    private String token;
}
