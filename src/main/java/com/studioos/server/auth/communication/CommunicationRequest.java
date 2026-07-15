package com.studioos.server.auth.communication;

import java.util.Map;

public record CommunicationRequest(
        CommunicationType type,
        String email,
        String phone,
        String subject,
        String message,
        Map<String, String> metadata
) {
    public CommunicationRequest {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
