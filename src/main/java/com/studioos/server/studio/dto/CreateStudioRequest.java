package com.studioos.server.studio.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateStudioRequest {

    @NotBlank(message = "Studio name is required")
    private String studioName;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Pricing is required")
    private Integer pricing;

    @NotBlank(message = "Availability is required")
    private String availability;

    @NotBlank(message = "Description is required")
    private String description;

    private String profileImage;

    private List<String> services;
}