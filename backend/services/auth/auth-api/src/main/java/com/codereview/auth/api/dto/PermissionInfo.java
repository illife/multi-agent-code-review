package com.codereview.auth.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Permission Info Response DTO
 *
 * @author Auth Service Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionInfo {

    private Long id;
    private String name;
    private String description;
    private String resource;
    private String action;
}
