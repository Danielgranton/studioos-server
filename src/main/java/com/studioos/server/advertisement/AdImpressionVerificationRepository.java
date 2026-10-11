package com.studioos.server.advertisement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.studioos.server.shared.enums.AdImpressionVerificationStatus;

import jakarta.persistence.LockModeType;

public interface AdImpressionVerificationRepository extends JpaRepository<AdImpressionVerification, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from AdImpressionVerification v where v.token = :token")
    Optional<AdImpressionVerification> findByTokenForUpdate(@Param("token") String token);

    @Modifying
    @Query("delete from AdImpressionVerification v where v.status = :status and v.expiresAt < :expiresBefore")
    int deleteExpired(@Param("status") AdImpressionVerificationStatus status,
                      @Param("expiresBefore") LocalDateTime expiresBefore);

    @Modifying
    @Query("delete from AdImpressionVerification v where v.status in :statuses and v.createdAt < :createdBefore")
    int deleteOldTerminal(@Param("statuses") List<AdImpressionVerificationStatus> statuses,
                          @Param("createdBefore") LocalDateTime createdBefore);
}
