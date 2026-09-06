package com.studioos.server.search.mapper;

import com.studioos.server.search.document.StudioDocument;
import com.studioos.server.search.dto.StudioSearchResult;
import com.studioos.server.studio.Studio;

public final class StudioMapper {
    private StudioMapper() {
    }

    public static StudioDocument toDocument(Studio studio, double avgRating, int ratingCount) {
        return StudioDocument.builder()
                .id(studio.getId())
                .studioName(studio.getStudioName())
                .location(studio.getLocation())
                .description(studio.getDescription())
                .badge(studio.getBadge())
                .genres(studio.getGenres())
                .equipment(studio.getEquipment())
                .rooms(studio.getRooms())
                .yearsActive(studio.getYearsActive())
                .responseTime(studio.getResponseTime())
                .available(studio.isAvailable())
                .nextAvailable(studio.getNextAvailable())
                .bookings(studio.getBookings())
                .verified(studio.isVerified())
                .pricing(studio.getPricing())
                .profileImageThumbnail(studio.getProfileImageThumbnail())
                .ownerId(studio.getOwnerId())
                .averageRating(avgRating)
                .ratingCount(ratingCount)
                .build();
    }

    public static StudioSearchResult toResult(StudioDocument doc, Double score) {
        return StudioSearchResult.builder()
                .id(doc.getId())
                .studioName(doc.getStudioName())
                .location(doc.getLocation())
                .pricing(doc.getPricing())
                .profileImageThumbnail(doc.getProfileImageThumbnail())
                .averageRating(doc.getAverageRating())
                .score(score)
                .build();
    }
}
