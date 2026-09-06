package com.studioos.server.studio.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudioResponse {
    private String id;
    private String studioName;
    private String location;
    private Integer pricing;
    private String availability;
    private String description;
    private String badge;
    private List<String> genres;
    private List<String> equipment;
    private Integer rooms;
    private Integer yearsActive;
    private String responseTime;
    private boolean available;
    private String nextAvailable;
    private Integer bookings;
    private boolean verified;
    private String profileImage;
    private String profileImageLarge;
    private String profileImageMedium;
    private String profileImageThumbnail;
    private Integer ownerId;
    private String ownerName;
    private String ownerProfileImageThumbnail;
    private List<String> services;
    private List<StudioMediaResponse> media;
    private Double averageRating;
    private Long totalRatings;
    private LocalDateTime createdAt;
}
