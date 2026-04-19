package com.think.platform.ci.auth.api.dto;

import lombok.Data;

/**
 * Login Response DTO
 *
 * @author Auth Service Team
 */
@Data
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private String username;
}
