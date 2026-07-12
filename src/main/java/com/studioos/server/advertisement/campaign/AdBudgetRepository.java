package com.studioos.server.advertisement.campaign;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdBudgetRepository extends JpaRepository<AdBudget, String> {
    Optional<AdBudget> findByCampaignId(String campaignId);
}