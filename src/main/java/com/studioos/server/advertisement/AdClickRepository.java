package com.studioos.server.advertisement;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdClickRepository extends JpaRepository<AdClick, String> {
    long countByCampaignId(String campaignId);
}
