package com.codereview.ai.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * User Info DTO
 *
 * @author Code Review AI Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String department;
    private Boolean isActive;
    private List<String> roles;
}
