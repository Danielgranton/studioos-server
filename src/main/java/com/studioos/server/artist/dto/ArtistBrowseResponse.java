package com.studioos.server.artist.dto;

import java.util.List;
import com.studioos.server.shared.enums.AvailabilityStatus;
import com.studioos.server.shared.enums.VerificationStatus;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ArtistBrowseResponse {
    Integer id;
    String name;
    String username;
    String location;
    String genre;
    String bio;
    String experience;
    String profileImage;
    String profileImageLarge;
    String profileImageMedium;
    String profileImageThumbnail;
    boolean verified;
    VerificationStatus verificationStatus;
    AvailabilityStatus availabilityStatus;
    double averageRating;
    long reviewCount;
    long followerCount;
    long releasedProjectCount;
    double popularityScore;
    double trendingScore;
    boolean featured;
    boolean available;
    List<String> specialties;
    List<ArtistServiceOfferingResponse> services;
}
