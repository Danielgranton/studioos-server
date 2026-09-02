package com.studioos.server.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivacySettingsRepository extends JpaRepository<PrivacySettings, Integer> {
    Optional<PrivacySettings> findByUserId(Integer userId);
}
