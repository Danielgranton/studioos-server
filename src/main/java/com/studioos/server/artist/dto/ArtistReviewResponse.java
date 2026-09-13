package com.studioos.server.artist.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ArtistReviewResponse {
    String id;
    Integer artistId;
    Integer reviewerId;
    String bookingId;
    Float rating;
    String review;
    LocalDateTime createdAt;
}
