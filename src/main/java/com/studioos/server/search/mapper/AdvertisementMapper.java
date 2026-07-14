package com.studioos.server.search.mapper;

import com.studioos.server.advertisement.Advertisement;
import com.studioos.server.advertisement.campaign.AdCampaign;
import com.studioos.server.search.document.AdvertisementDocument;
import com.studioos.server.search.dto.AdvertisementSearchResult;

public final class AdvertisementMapper {
    private AdvertisementMapper() {
    }

    public static AdvertisementDocument toDocument(Advertisement advertisement, AdCampaign campaign) {
        return AdvertisementDocument.builder()
                .id(advertisement.getId())
                .campaignId(advertisement.getCampaignId())
                .advertiserId(campaign != null ? campaign.getAdvertiserId() : null)
                .campaignTitle(campaign != null ? campaign.getTitle() : null)
                .type(advertisement.getType())
                .headline(advertisement.getHeadline())
                .description(advertisement.getDescription())
                .ctaText(advertisement.getCtaText())
                .ctaUrl(advertisement.getCtaUrl())
                .mediaUrl(advertisement.getMediaUrl())
                .thumbnailUrl(advertisement.getThumbnailUrl())
                .duration(advertisement.getDuration())
                .status(advertisement.getStatus())
                .createdAt(advertisement.getCreatedAt() != null ? advertisement.getCreatedAt().toString() : null)
                .build();
    }

    public static AdvertisementSearchResult toResult(AdvertisementDocument doc, Double score) {
        return AdvertisementSearchResult.builder()
                .id(doc.getId())
                .campaignId(doc.getCampaignId())
                .headline(doc.getHeadline())
                .description(doc.getDescription())
                .type(doc.getType())
                .status(doc.getStatus())
                .score(score)
                .build();
    }
}
