package com.studioos.server.studio.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreateStudioRequest {

    @NotBlank(message = "Studio name is required")
    private String studioName;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Pricing is required")
    private Integer pricing;
    @NotNull(message = "Full-production package price is required")
    @Positive(message = "Full-production package price must be greater than zero")
    private Integer productionPackagePrice;

    @NotBlank(message = "Availability is required")
    private String availability;

    @NotBlank(message = "Description is required")
    private String description;

    private String badge;
    private List<String> genres;
    private List<String> equipment;
    private Integer rooms;
    private Integer yearsActive;
    private String responseTime;
    private Boolean available;
    private String nextAvailable;

    private String profileImage;

    private List<String> services;
    private List<StudioServiceOfferingRequest> serviceDetails;
}
