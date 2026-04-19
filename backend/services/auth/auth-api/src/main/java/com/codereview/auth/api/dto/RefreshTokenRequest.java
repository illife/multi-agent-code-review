package com.codereview.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Refresh Token Request DTO
 *
 * @author Auth Service Team
 */
@Data
public class RefreshTokenRequest {

    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;
}
