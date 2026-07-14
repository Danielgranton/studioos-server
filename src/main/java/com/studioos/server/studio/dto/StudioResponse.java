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
    private String profileImage;
    private String profileImageLarge;
    private String profileImageMedium;
    private String profileImageThumbnail;
    private Integer ownerId;
    private String ownerName;
    private List<String> services;
    private Double averageRating;
    private Long totalRatings;
    private LocalDateTime createdAt;
}
