package com.studioos.server.advertisement.campaign;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdBudgetRepository extends JpaRepository<AdBudget, String> {
    Optional<AdBudget> findByCampaignId(String campaignId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update AdBudget b set b.remainingBudget = b.remainingBudget - :cost, "
            + "b.spentBudget = b.spentBudget + :cost "
            + "where b.campaignId = :campaignId and b.remainingBudget >= :cost")
    int debitForVerifiedImpression(@Param("campaignId") String campaignId, @Param("cost") int cost);
}
