package com.think.platform.shared.common.dto;

import com.think.platform.shared.common.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改密码请求DTO
 *
 * @author Knowledge Base Team
 */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "当前密码不能为空")
    private String currentPassword;

    @NotBlank(message = "新密码不能为空")
    @ValidPassword
    private String newPassword;
}
