package com.studioos.server.servicebooking.dto;

import java.time.LocalDateTime;

import com.studioos.server.servicebooking.ServiceBookingStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceBookingResponse {
    private String id;
    private Integer providerId;
    private String providerType;
    private String providerName;
    private String listingId;
    private String studioId;
    private String serviceName;
    private Integer requesterId;
    private String requesterName;
    private LocalDateTime preferredDate;
    private String requestDetails;
    private Integer amount;
    private String currency;
    private String transactionId;
    private ServiceBookingStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
