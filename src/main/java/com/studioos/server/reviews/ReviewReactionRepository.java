package com.studioos.server.reviews;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewReactionRepository extends JpaRepository<ReviewReaction, String> {
    Optional<ReviewReaction> findByReviewTypeAndReviewIdAndUserId(
            ReviewType reviewType, String reviewId, Integer userId);

    long countByReviewTypeAndReviewIdAndReaction(
            ReviewType reviewType, String reviewId, ReviewReactionType reaction);
}
