package com.studioos.server.artist.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateArtistServiceRequest {
    @Size(max = 1000, message = "Request note must not exceed 1000 characters")
    private String requestNote;
}
