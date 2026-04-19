package com.think.platform.ci.auth.api.dto;

import lombok.Data;

/**
 * Login Request DTO
 *
 * @author Auth Service Team
 */
@Data
public class LoginRequest {
    private String username;
    private String password;
}
