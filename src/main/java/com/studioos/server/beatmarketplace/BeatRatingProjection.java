package com.studioos.server.beatmarketplace;

public interface BeatRatingProjection {
    String getBeatId();
    Double getAverageRating();
    Long getReviewCount();
}
