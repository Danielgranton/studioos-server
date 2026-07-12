package com.studioos.server.advertisement.campaign;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.advertisement.AdNotificationService;
import com.studioos.server.shared.enums.AdCampaignStatus;
import com.studioos.server.shared.enums.AdPaymentStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdCampaignActivationScheduler {

    private final AdCampaignRepository adCampaignRepository;
    private final AdNotificationService adNotificationService;

    @Scheduled(fixedDelayString = "${ads.campaign-activation-interval-ms:60000}")
    @Transactional
    public void activateDueCampaigns() {
        LocalDateTime now = LocalDateTime.now();

        adCampaignRepository
                .findByStatusAndPaymentStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        AdCampaignStatus.SCHEDULED,
                        AdPaymentStatus.PAID,
                        now,
                        now)
                .forEach(campaign -> {
                    campaign.setStatus(AdCampaignStatus.ACTIVE);
                    adCampaignRepository.save(campaign);
                    log.info("Activated scheduled ad campaign {}", campaign.getId());
                    adNotificationService.notifyCampaignActivated(campaign);
                });
    }
}
