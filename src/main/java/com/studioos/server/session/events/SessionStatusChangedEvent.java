package com.studioos.server.session.events;

import com.studioos.server.shared.enums.SessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SessionStatusChangedEvent {
    private String sessionId;
    private SessionStatus status;
}
