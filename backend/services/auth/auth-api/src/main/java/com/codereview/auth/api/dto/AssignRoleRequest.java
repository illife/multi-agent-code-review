package com.codereview.auth.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Assign Role Request DTO
 *
 * @author Auth Service Team
 */
@Data
public class AssignRoleRequest {

    @NotNull(message = "角色ID不能为空")
    private Long roleId;
}
