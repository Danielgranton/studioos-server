package com.studioos.server.artist.dto;

import java.time.LocalDateTime;

import com.studioos.server.artist.ArtistServiceRequestStatus;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ArtistServiceRequestResponse {
    String id;
    String serviceId;
    String serviceName;
    Integer requesterId;
    String requesterName;
    String requestNote;
    Integer amount;
    String currency;
    ArtistServiceRequestStatus status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
