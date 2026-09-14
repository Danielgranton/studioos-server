package com.studioos.server.reviews.dto;

import com.studioos.server.reviews.ReviewReactionType;
import com.studioos.server.reviews.ReviewType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReviewInteractionResponse {
    ReviewType reviewType;
    String reviewId;
    ReviewReactionType currentReaction;
    long likes;
    long dislikes;
    long comments;
}
