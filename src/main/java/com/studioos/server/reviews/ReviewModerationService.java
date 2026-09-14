package com.studioos.server.reviews;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.artist.ArtistReview;
import com.studioos.server.artist.ArtistReviewRepository;
import com.studioos.server.reviews.dto.ReviewModerationRequest;
import com.studioos.server.reviews.dto.ReviewReportRequest;
import com.studioos.server.reviews.dto.ReviewReportResponse;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.studio.StudioRating;
import com.studioos.server.studio.StudioRatingRepository;
import com.studioos.server.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewModerationService {

    private final ArtistReviewRepository artistReviewRepository;
    private final ProducerReviewRepository producerReviewRepository;
    private final StudioRatingRepository studioRatingRepository;
    private final ReviewReportRepository reportRepository;

    @Transactional
    public ReviewReportResponse report(User reporter, ReviewType type, String reviewId, ReviewReportRequest request) {
        ensureActiveReview(type, reviewId);
        if (reportRepository.findByReviewTypeAndReviewIdAndReporterId(type, reviewId, reporter.getId()).isPresent()) {
            throw StudioosException.conflict("You have already reported this review");
        }
        ReviewReport report = reportRepository.save(ReviewReport.builder()
                .reviewType(type)
                .reviewId(reviewId)
                .reporterId(reporter.getId())
                .reason(request.getReason().trim())
                .details(request.getDetails() == null ? null : request.getDetails().trim())
                .build());
        return toResponse(report);
    }

    @Transactional(readOnly = true)
    public Page<ReviewReportResponse> getPendingReports(Pageable pageable) {
        return reportRepository.findByStatus(ReviewReportStatus.PENDING, pageable).map(this::toResponse);
    }

    @Transactional
    public ReviewReportResponse resolve(User moderator, String reportId, ReviewModerationRequest request) {
        ReviewReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> StudioosException.notFound("Review report not found"));
        if (report.getStatus() != ReviewReportStatus.PENDING) {
            throw StudioosException.conflict("Review report has already been resolved");
        }
        if (request.getStatus() == ReviewModerationStatus.ACTIVE) {
            report.setStatus(ReviewReportStatus.DISMISSED);
        } else {
            moderateReview(report.getReviewType(), report.getReviewId(), request, moderator.getId());
            report.setStatus(ReviewReportStatus.RESOLVED);
        }
        report.setReviewedBy(moderator.getId());
        report.setReviewedAt(LocalDateTime.now());
        report.setResolution(request.getResolution());
        return toResponse(reportRepository.save(report));
    }

    private void ensureActiveReview(ReviewType type, String reviewId) {
        boolean exists = switch (type) {
            case ARTIST -> artistReviewRepository.findByIdAndModerationStatus(reviewId, ReviewModerationStatus.ACTIVE).isPresent();
            case PRODUCER -> producerReviewRepository.findByIdAndModerationStatus(reviewId, ReviewModerationStatus.ACTIVE).isPresent();
            case STUDIO -> studioRatingRepository.findByIdAndModerationStatus(reviewId, ReviewModerationStatus.ACTIVE).isPresent();
        };
        if (!exists) throw StudioosException.notFound("Review not found");
    }

    private void moderateReview(ReviewType type, String reviewId, ReviewModerationRequest request, Integer moderatorId) {
        switch (type) {
            case ARTIST -> artistReviewRepository.findById(reviewId).ifPresentOrElse(review -> {
                review.setModerationStatus(request.getStatus());
                review.setModeratedAt(LocalDateTime.now());
                review.setModeratedBy(moderatorId);
                review.setModerationReason(request.getResolution());
                artistReviewRepository.save(review);
            }, () -> { throw StudioosException.notFound("Review not found"); });
            case PRODUCER -> producerReviewRepository.findById(reviewId).ifPresentOrElse(review -> {
                review.setModerationStatus(request.getStatus());
                review.setModeratedAt(LocalDateTime.now());
                review.setModeratedBy(moderatorId);
                review.setModerationReason(request.getResolution());
                producerReviewRepository.save(review);
            }, () -> { throw StudioosException.notFound("Review not found"); });
            case STUDIO -> studioRatingRepository.findById(reviewId).ifPresentOrElse(review -> {
                review.setModerationStatus(request.getStatus());
                review.setModeratedAt(LocalDateTime.now());
                review.setModeratedBy(moderatorId);
                review.setModerationReason(request.getResolution());
                studioRatingRepository.save(review);
            }, () -> { throw StudioosException.notFound("Review not found"); });
        }
    }

    private ReviewReportResponse toResponse(ReviewReport report) {
        return ReviewReportResponse.builder()
                .id(report.getId()).reviewType(report.getReviewType()).reviewId(report.getReviewId())
                .reporterId(report.getReporterId()).reason(report.getReason()).details(report.getDetails())
                .status(report.getStatus()).reviewedBy(report.getReviewedBy()).reviewedAt(report.getReviewedAt())
                .resolution(report.getResolution()).createdAt(report.getCreatedAt()).build();
    }
}
