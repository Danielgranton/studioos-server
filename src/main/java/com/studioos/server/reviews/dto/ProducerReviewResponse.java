package com.studioos.server.reviews.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ProducerReviewResponse {
    private String id;
    private Integer producerId;
    private Integer reviewerId;
    private String bookingId;
    private Float rating;
    private String review;
    private LocalDateTime createdAt;
}
