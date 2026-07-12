package com.studioos.server.booking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ConfirmBookingRequest {
    @NotNull
    @Positive
    private Integer totalPrice;
}
