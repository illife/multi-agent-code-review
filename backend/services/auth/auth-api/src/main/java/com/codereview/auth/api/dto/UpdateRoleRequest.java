package com.codereview.auth.api.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Update Role Request DTO
 *
 * @author Auth Service Team
 */
@Data
public class UpdateRoleRequest {

    @Size(min = 3, max = 50, message = "角色名称长度必须在3-50之间")
    private String name;

    private String description;
}
