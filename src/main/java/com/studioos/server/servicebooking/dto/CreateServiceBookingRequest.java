package com.studioos.server.servicebooking.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateServiceBookingRequest {
    @NotBlank private String providerType;
    @NotNull private Integer providerId;
    @NotBlank private String listingId;
    private String studioId;
    @NotBlank @Size(max = 120) private String serviceName;
    private String catalogServiceId;
    @NotNull @Future private LocalDateTime preferredDate;
    @NotBlank @Size(max = 2000) private String requestDetails;
}
