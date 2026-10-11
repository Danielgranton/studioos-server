package com.studioos.server.booking.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.studioos.server.shared.enums.BookingPaymentStatus;
import com.studioos.server.shared.enums.BookingStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private String id;
    private String studioId;
    private String studioName;
    private Integer artistId;
    private String artistName;
    private LocalDateTime sessionDate;
    private List<String> packageServices;
    private Integer attemptCount;
    private Integer productionPackagePrice;
    private Integer totalPrice;
    private BookingStatus status;
    private BookingPaymentStatus paymentStatus;
    private String notes;
    private LocalDateTime createdAt;
}
