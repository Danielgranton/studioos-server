package com.studioos.server.artist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ArtistServiceOfferingRequest {
    @NotBlank(message = "Service name is required")
    @Size(max = 120, message = "Service name must not exceed 120 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Price is required")
    @PositiveOrZero(message = "Price cannot be negative")
    private Integer price;

    @Pattern(regexp = "[A-Z]{3}", message = "Currency must be a three-letter uppercase code")
    private String currency = "KES";

    private Boolean active = true;
}
