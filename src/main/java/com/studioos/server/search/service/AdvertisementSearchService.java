package com.studioos.server.search.service;

import com.studioos.server.advertisement.AdvertisementRepository;
import com.studioos.server.advertisement.campaign.AdCampaignRepository;
import com.studioos.server.search.dto.AdvertisementSearchResult;
import com.studioos.server.search.mapper.AdvertisementMapper;
import com.studioos.server.search.util.SearchSanitizer;
import com.studioos.server.shared.enums.AdCreativeStatus;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdvertisementSearchService {

    private final AdvertisementRepository advertisementRepository;
    private final AdCampaignRepository adCampaignRepository;

    public List<AdvertisementSearchResult> search(String query, int page, int size) {
        String needle = SearchSanitizer.sanitize(query);
        return advertisementRepository.findAll().stream()
                .filter(ad -> matches(ad.getHeadline(), needle)
                        || matches(ad.getDescription(), needle)
                        || matches(adCampaignRepository.findById(ad.getCampaignId()).map(c -> c.getTitle()).orElse(null), needle))
                .map(ad -> AdvertisementMapper.toDocument(ad, adCampaignRepository.findById(ad.getCampaignId()).orElse(null)))
                .map(doc -> AdvertisementMapper.toResult(doc, score(doc, needle)))
                .filter(result -> result.getStatus() == AdCreativeStatus.READY
                        || result.getStatus() == AdCreativeStatus.RUNNING
                        || result.getStatus() == AdCreativeStatus.PENDING_REVIEW)
                .sorted(Comparator.comparingDouble(AdvertisementSearchResult::getScore).reversed())
                .skip((long) page * size)
                .limit(size)
                .toList();
    }

    private boolean matches(String value, String needle) {
        return needle.isBlank() || (value != null && value.toLowerCase().contains(needle));
    }

    private double score(com.studioos.server.search.document.AdvertisementDocument doc, String needle) {
        double titleBoost = matches(doc.getHeadline(), needle) ? 1.0 : 0.0;
        double campaignBoost = matches(doc.getCampaignTitle(), needle) ? 0.6 : 0.0;
        double descBoost = matches(doc.getDescription(), needle) ? 0.3 : 0.0;
        return titleBoost + campaignBoost + descBoost;
    }
}
