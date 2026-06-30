package com.studioos.server.auth.otp;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findTopByIdentifierAndUsedFalseOrderByCreatedAtDesc(String identifier);

    @Modifying
    @Transactional
    @Query("UPDATE Otp o SET o.used = true WHERE o.identifier = :identifier")
    void invalidateAllForIdentifier(String identifier);
}