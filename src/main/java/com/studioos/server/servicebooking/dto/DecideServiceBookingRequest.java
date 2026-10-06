package com.studioos.server.servicebooking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DecideServiceBookingRequest {
    @NotNull private Boolean accepted;
    @Min(1) private Integer amount;
}
