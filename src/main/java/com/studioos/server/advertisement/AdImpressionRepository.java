package com.studioos.server.advertisement;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdImpressionRepository extends JpaRepository<AdImpression, String> {
    long countByCampaignId(String campaignId);
    long countByAdvertisementIdAndUserIdAndOccurredAtAfter(String advertisementId, Integer userId, LocalDateTime occurredAtAfter);
}
