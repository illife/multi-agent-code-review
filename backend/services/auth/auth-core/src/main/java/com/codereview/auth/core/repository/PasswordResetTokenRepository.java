package com.codereview.auth.core.repository;

import com.codereview.auth.core.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Password Reset Token Repository - Auth Service
 *
 * @author Auth Service Team
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    @Query("SELECT prt FROM PasswordResetToken prt WHERE prt.user.id = :userId AND prt.used = false AND prt.expiresAt > :now ORDER BY prt.createdAt DESC")
    List<PasswordResetToken> findValidTokensByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE PasswordResetToken prt SET prt.used = true WHERE prt.user.id = :userId")
    void invalidateAllUserTokens(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.expiresAt < :dateTime OR prt.used = true")
    void deleteExpiredOrUsedTokens(@Param("dateTime") LocalDateTime dateTime);

    @Modifying
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
