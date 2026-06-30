package com.studioos.server.booking.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreateBookingRequest {

    @NotBlank(message = "Studio ID is required")
    private String studioId;

    @NotNull(message = "Session date is required")
    private LocalDateTime sessionDate;

    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be positive")
    private Integer durationHours;

    private String notes;
}