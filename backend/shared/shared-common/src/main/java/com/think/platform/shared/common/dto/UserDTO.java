package com.think.platform.shared.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User DTO - Shared across all services
 *
 * This DTO is used for service-to-service communication
 * to avoid each service having its own User entity.
 *
 * @author Platform Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String role; // USER, ADMIN
    private Boolean isActive;
}
