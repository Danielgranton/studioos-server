package com.studioos.server.artist.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RateArtistRequest {
    @NotBlank
    private String bookingId;

    @NotNull
    @DecimalMin("1.0")
    @DecimalMax("5.0")
    private Float rating;

    @Size(max = 2000)
    private String review;
}
