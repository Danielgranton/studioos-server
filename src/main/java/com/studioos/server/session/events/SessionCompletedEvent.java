package com.studioos.server.session.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SessionCompletedEvent {
    private String sessionId;
    private String bookingId;
}
