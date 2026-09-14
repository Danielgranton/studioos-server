package com.studioos.server.reviews.dto;

import java.time.LocalDateTime;

import com.studioos.server.reviews.ReviewReportStatus;
import com.studioos.server.reviews.ReviewType;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReviewReportResponse {
    String id;
    ReviewType reviewType;
    String reviewId;
    Integer reporterId;
    String reason;
    String details;
    ReviewReportStatus status;
    Integer reviewedBy;
    LocalDateTime reviewedAt;
    String resolution;
    LocalDateTime createdAt;
}
