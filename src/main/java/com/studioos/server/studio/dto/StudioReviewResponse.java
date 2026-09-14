package com.studioos.server.studio.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudioReviewResponse {
    String id;
    String studioId;
    Integer reviewerId;
    String reviewerName;
    String reviewerUsername;
    String reviewerRole;
    String reviewerAvatar;
    String bookingId;
    Float rating;
    String review;
    long likes;
    long comments;
    long dislikes;
    LocalDateTime createdAt;
}
