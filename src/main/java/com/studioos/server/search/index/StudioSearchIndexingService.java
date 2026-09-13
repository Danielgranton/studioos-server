package com.studioos.server.search.index;

import com.studioos.server.search.document.StudioDocument;
import com.studioos.server.studio.Studio;
import com.studioos.server.engagement.PopularityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudioSearchIndexingService {

    private static final String INDEX_NAME = "studios";

    private final OpenSearchClient openSearchClient;
    private final PopularityService popularityService;

    public void indexStudio(Studio studio) {
        try {
            double avgRating = studio.getRatings() == null || studio.getRatings().isEmpty()
                    ? 0.0
                    : studio.getRatings().stream()
                            .mapToDouble(r -> r.getRating())
                            .average()
                            .orElse(0.0);

            StudioDocument doc = StudioDocument.builder()
                    .id(studio.getId())
                    .studioName(studio.getStudioName())
                    .location(studio.getOwner() != null && studio.getOwner().getLocation() != null
                            ? studio.getOwner().getLocation()
                            : studio.getLocation())
                    .description(studio.getDescription())
                    .badge(studio.getBadge())
                    .genres(studio.getGenres())
                    .equipment(studio.getEquipment())
                    .rooms(studio.getRooms())
                    .yearsActive(studio.getYearsActive())
                    .responseTime(studio.getResponseTime())
                    .available(studio.getOwner() == null
                            ? studio.isAvailable()
                            : studio.getOwner().isAvailable())
                    .nextAvailable(studio.getNextAvailable())
                    .bookings(studio.getBookings())
                    .verified(studio.isVerified())
                    .pricing(studio.getPricing())
                    .profileImageThumbnail(studio.getProfileImageThumbnail())
                    .ownerId(studio.getOwnerId())
                    .averageRating(avgRating)
                    .ratingCount(studio.getRatings() != null ? studio.getRatings().size() : 0)
                    .popularityScore(popularityService.studioScore(studio.getId()))
                    .trendingScore(popularityService.studioTrendingScore(studio.getId()))
                    .featured(studio.isFeatured())
                    .build();

            openSearchClient.index(i -> i.index(INDEX_NAME).id(studio.getId()).document(doc));
        } catch (Exception e) {
            log.error("Failed to index studio {} in OpenSearch: {}", studio.getId(), e.getMessage());
        }
    }
}
