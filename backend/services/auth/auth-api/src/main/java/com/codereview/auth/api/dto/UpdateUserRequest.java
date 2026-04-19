package com.codereview.auth.api.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Update User Request DTO
 *
 * @author Auth Service Team
 */
@Data
public class UpdateUserRequest {

    @Size(max = 100, message = "姓名长度不能超过100")
    private String fullName;

    @Size(max = 100, message = "部门长度不能超过100")
    private String department;
}
