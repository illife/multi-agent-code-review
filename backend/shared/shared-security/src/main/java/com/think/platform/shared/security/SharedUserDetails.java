package com.think.platform.shared.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared User Details
 * Merged from codereview-api and knowledge-base-api
 *
 * 包装用户信息为 Spring Security UserDetails
 * 支持动态角色和权限
 *
 * @author AI Code Mentor Team
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedUserDetails implements UserDetails {

    private Long id;
    private String username;
    private String email;
    private String password;
    private Set<String> roles = new HashSet<>();
    private boolean enabled = true;
    private boolean accountNonExpired = true;
    private boolean accountNonLocked = true;
    private boolean credentialsNonExpired = true;

    /**
     * 从用户实体创建 SharedUserDetails (基础版本)
     */
    public static SharedUserDetails create(Long id, String username, String email, String password) {
        SharedUserDetails userDetails = new SharedUserDetails();
        userDetails.setId(id);
        userDetails.setUsername(username);
        userDetails.setEmail(email);
        userDetails.setPassword(password);
        return userDetails;
    }

    /**
     * 从用户实体创建 SharedUserDetails (带角色)
     */
    public static SharedUserDetails create(Long id, String username, String email, String password, Collection<String> roles) {
        SharedUserDetails userDetails = new SharedUserDetails();
        userDetails.setId(id);
        userDetails.setUsername(username);
        userDetails.setEmail(email);
        userDetails.setPassword(password);
        userDetails.setRoles(new HashSet<>(roles));
        return userDetails;
    }

    /**
     * 从用户实体创建 SharedUserDetails (完整版本)
     */
    public static SharedUserDetails create(Long id, String username, String email, String password,
                                          Collection<String> roles, boolean enabled, boolean accountNonLocked) {
        SharedUserDetails userDetails = new SharedUserDetails();
        userDetails.setId(id);
        userDetails.setUsername(username);
        userDetails.setEmail(email);
        userDetails.setPassword(password);
        userDetails.setRoles(roles != null ? new HashSet<>(roles) : new HashSet<>());
        userDetails.setEnabled(enabled);
        userDetails.setAccountNonLocked(accountNonLocked);
        return userDetails;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 检查是否有指定角色
     */
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /**
     * 检查是否有任意一个指定角色
     */
    public boolean hasAnyRole(String... rolesToCheck) {
        for (String role : rolesToCheck) {
            if (roles.contains(role)) {
                return true;
            }
        }
        return false;
    }
}
