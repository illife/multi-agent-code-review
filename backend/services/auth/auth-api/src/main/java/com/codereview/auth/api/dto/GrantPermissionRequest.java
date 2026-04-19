package com.codereview.auth.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Grant Permission Request DTO
 *
 * @author Auth Service Team
 */
@Data
public class GrantPermissionRequest {

    @NotNull(message = "权限ID不能为空")
    private Long permissionId;
}
