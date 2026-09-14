package com.studioos.server.reviews;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, String> {
    Optional<ReviewReport> findByReviewTypeAndReviewIdAndReporterId(
            ReviewType reviewType, String reviewId, Integer reporterId);

    Page<ReviewReport> findByStatus(ReviewReportStatus status, Pageable pageable);
}
