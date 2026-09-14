package com.studioos.server.artist.dto;

import com.studioos.server.artist.ArtistServiceRequestStatus;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateArtistServiceRequest {
    @NotNull(message = "Request status is required")
    private ArtistServiceRequestStatus status;
}
