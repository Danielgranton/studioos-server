package com.studioos.server.booking.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreateBookingRequest {

    @NotBlank(message = "Studio ID is required")
    private String studioId;

    @NotNull(message = "Session date is required")
    @Future(message = "Session date must be in the future")
    private LocalDateTime sessionDate;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least one hour")
    @Max(value = 12, message = "Duration cannot exceed 12 hours")
    @Positive(message = "Duration must be positive")
    private Integer durationHours;

    private String notes;
}
