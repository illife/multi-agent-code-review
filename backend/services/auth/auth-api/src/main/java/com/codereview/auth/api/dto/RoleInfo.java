package com.codereview.auth.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Role Info Response DTO
 *
 * @author Auth Service Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleInfo {

    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private Set<String> permissions;
}
