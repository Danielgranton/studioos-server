package com.studioos.server.artist.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ArtistServiceOfferingResponse {
    String id;
    Integer artistId;
    String name;
    String description;
    Integer price;
    String currency;
    boolean active;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
