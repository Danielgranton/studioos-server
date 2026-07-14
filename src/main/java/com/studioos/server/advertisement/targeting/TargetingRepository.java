package com.studioos.server.advertisement.targeting;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TargetingRepository extends JpaRepository<Targeting, String> {
    Optional<Targeting> findByCampaignId(String campaignId);
    boolean existsByCampaignId(String campaignId);
}
