package com.think.platform.km.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简化版用户信息类
 * 用于KM服务内部，替代已删除的User实体
 *
 * @author Knowledge Base Team
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

    /**
     * 从Auth服务的用户信息转换
     * @param userId 用户ID
     * @param username 用户名
     * @param email 邮箱
     * @return 用户信息
     */
    public static UserInfo of(Long userId, String username, String email, String fullName) {
        return UserInfo.builder()
                .id(userId)
                .username(username)
                .email(email)
                .fullName(fullName)
                .build();
    }
}
