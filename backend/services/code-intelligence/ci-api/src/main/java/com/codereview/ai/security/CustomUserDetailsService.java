package com.codereview.ai.security;

import com.codereview.ai.domain.model.User;
import com.codereview.ai.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom UserDetailsService implementation
 * Matches the pattern from knowledge-base-api
 *
 * @author Code Review AI Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        User user = userRepository.findByUsernameWithRoles(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!user.getIsActive()) {
            throw new UsernameNotFoundException("User is disabled: " + username);
        }

        log.debug("User loaded: {}, roles: {}", username, user.getRoles());

        return CustomUserDetails.create(user);
    }

    /**
     * Load user by ID
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + id));

        if (!user.getIsActive()) {
            throw new UsernameNotFoundException("User is disabled: " + id);
        }

        return CustomUserDetails.create(user);
    }
}
