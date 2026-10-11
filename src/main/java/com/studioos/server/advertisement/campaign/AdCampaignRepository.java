package com.studioos.server.advertisement.campaign;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import com.studioos.server.shared.enums.AdCampaignStatus;
import com.studioos.server.shared.enums.AdPaymentStatus;

public interface AdCampaignRepository extends JpaRepository<AdCampaign, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AdCampaign c where c.id = :campaignId")
    java.util.Optional<AdCampaign> findByIdForUpdate(@Param("campaignId") String campaignId);

    List<AdCampaign> findByAdvertiserId(Integer advertiserId);
    Page<AdCampaign> findByAdvertiserId(Integer advertiserId, Pageable pageable);
    List<AdCampaign> findByStudioId(String studioId);
    java.util.Optional<AdCampaign> findByTransactionId(String transactionId);
    List<AdCampaign> findByStatusAndPaymentStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            AdCampaignStatus status,
            AdPaymentStatus paymentStatus,
            LocalDateTime startDate,
            LocalDateTime endDate);
    boolean existsByStudioId(String studioId);
}
