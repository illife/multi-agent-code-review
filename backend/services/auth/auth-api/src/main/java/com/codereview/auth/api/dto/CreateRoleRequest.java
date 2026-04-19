package com.codereview.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Create Role Request DTO
 *
 * @author Auth Service Team
 */
@Data
public class CreateRoleRequest {

    @NotBlank(message = "角色名称不能为空")
    @Size(min = 3, max = 50, message = "角色名称长度必须在3-50之间")
    private String name;

    private String description;
}
