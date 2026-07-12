package com.studioos.server.advertisement.campaign;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdCampaignRepository extends JpaRepository<AdCampaign, String> {
    List<AdCampaign> findByAdvertiserId(Integer advertiserId);
    List<AdCampaign> findByStudioId(String studioId);
    boolean existsByStudioId(String studioId);
}
