package com.think.platform.shared.common.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 认证响应DTO
 *
 * @author AI Code Mentor Team
 */
@Data
public class AuthResponse {

    private String accessToken;

    private String refreshToken;

    private String tokenType = "Bearer";

    private Long userId;

    private String username;

    private String email;

    private String role;

    public static AuthResponse of(String accessToken, String refreshToken, Long userId, String username, String role) {
        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setUserId(userId);
        response.setUsername(username);
        response.setRole(role);
        return response;
    }
}
