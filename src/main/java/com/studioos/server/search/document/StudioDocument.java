package com.studioos.server.search.document;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudioDocument {
    private String id;
    private String studioName;
    private String location;
    private String description;
    private String badge;
    private List<String> genres;
    private List<String> equipment;
    private Integer rooms;
    private Integer yearsActive;
    private String responseTime;
    private Boolean available;
    private String nextAvailable;
    private Integer bookings;
    private Boolean verified;
    private Integer pricing;
    private String profileImageThumbnail;
    private Integer ownerId;
    private Double averageRating;
    private Integer ratingCount;
    private Double popularityScore;
    private Double trendingScore;
    private Boolean featured;
}
