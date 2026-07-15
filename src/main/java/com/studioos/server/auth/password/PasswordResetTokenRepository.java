package com.studioos.server.auth.password;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findTopByTokenHashAndUsedFalseOrderByCreatedAtDesc(String tokenHash);
}
