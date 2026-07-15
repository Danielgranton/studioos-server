package com.studioos.server.auth.session;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshSessionRepository extends JpaRepository<RefreshSession, String> {
    Optional<RefreshSession> findByTokenHash(String tokenHash);
    Optional<RefreshSession> findByTokenHashAndRevokedAtIsNull(String tokenHash);
    List<RefreshSession> findByUserId(Integer userId);
    List<RefreshSession> findByUserIdAndRevokedAtIsNull(Integer userId);
    List<RefreshSession> findByUserIdOrderByCreatedAtDesc(Integer userId);
}
