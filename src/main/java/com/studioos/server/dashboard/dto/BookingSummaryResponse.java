package com.studioos.server.dashboard.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BookingSummaryResponse {
    private String bookingId;
    private String studioName;
    private LocalDateTime sessionDate;
    private String status;
    private Integer totalPrice;
    private String producerName;
}