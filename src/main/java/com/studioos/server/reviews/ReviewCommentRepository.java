package com.studioos.server.reviews;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewCommentRepository extends JpaRepository<ReviewComment, String> {
    Page<ReviewComment> findByReviewTypeAndReviewIdAndDeletedAtIsNull(
            ReviewType reviewType, String reviewId, Pageable pageable);

    long countByReviewTypeAndReviewIdAndDeletedAtIsNull(ReviewType reviewType, String reviewId);
}
