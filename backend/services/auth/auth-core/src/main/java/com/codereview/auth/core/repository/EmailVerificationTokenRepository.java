package com.codereview.auth.core.repository;

import com.codereview.auth.core.domain.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Email verification token repository.
 */
@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    @Modifying
    @Query("UPDATE EmailVerificationToken evt SET evt.used = true WHERE evt.user.id = :userId")
    void invalidateAllUserTokens(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken evt WHERE evt.expiresAt < :dateTime OR evt.used = true")
    void deleteExpiredOrUsedTokens(@Param("dateTime") LocalDateTime dateTime);
}
