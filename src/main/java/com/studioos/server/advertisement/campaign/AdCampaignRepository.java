package com.studioos.server.advertisement.campaign;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.studioos.server.shared.enums.AdCampaignStatus;
import com.studioos.server.shared.enums.AdPaymentStatus;

public interface AdCampaignRepository extends JpaRepository<AdCampaign, String> {
    List<AdCampaign> findByAdvertiserId(Integer advertiserId);
    List<AdCampaign> findByStudioId(String studioId);
    java.util.Optional<AdCampaign> findByTransactionId(String transactionId);
    List<AdCampaign> findByStatusAndPaymentStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            AdCampaignStatus status,
            AdPaymentStatus paymentStatus,
            LocalDateTime startDate,
            LocalDateTime endDate);
    boolean existsByStudioId(String studioId);
}
