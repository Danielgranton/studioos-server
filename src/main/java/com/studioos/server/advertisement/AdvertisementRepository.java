package com.studioos.server.advertisement;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvertisementRepository extends JpaRepository<Advertisement, String> {
    List<Advertisement> findByCampaignId(String campaignId);
}