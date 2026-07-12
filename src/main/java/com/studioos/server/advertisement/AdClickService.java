package com.studioos.server.advertisement;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdClickService {

    private final AdvertisementRepository advertisementRepository;
    private final AdClickRepository adClickRepository;

    @Transactional
    public String recordClickAndGetRedirectUrl(String advertisementId, Integer userId) {
        Advertisement ad = advertisementRepository.findById(advertisementId)
                .orElseThrow(() -> new IllegalArgumentException("Advertisement not found: " + advertisementId));

        AdClick click = AdClick.builder()
                .advertisementId(ad.getId())
                .campaignId(ad.getCampaignId())
                .userId(userId)
                .build();
        adClickRepository.save(click);

        return ad.getCtaUrl();
    }
}