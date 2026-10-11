package com.studioos.server.advertisement;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface AdvertisementRepository extends JpaRepository<Advertisement, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Advertisement a where a.id = :advertisementId")
    java.util.Optional<Advertisement> findByIdForUpdate(@Param("advertisementId") String advertisementId);

    List<Advertisement> findByCampaignId(String campaignId);
    List<Advertisement> findByCampaignIdOrderByCreatedAtDesc(String campaignId);
    long countByCampaignId(String campaignId);
}
