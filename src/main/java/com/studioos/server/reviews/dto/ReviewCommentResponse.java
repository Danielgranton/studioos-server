package com.studioos.server.reviews.dto;

import java.time.LocalDateTime;

import com.studioos.server.reviews.ReviewType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReviewCommentResponse {
    String id;
    ReviewType reviewType;
    String reviewId;
    Integer userId;
    String userName;
    String username;
    String avatar;
    String body;
    LocalDateTime createdAt;
}
