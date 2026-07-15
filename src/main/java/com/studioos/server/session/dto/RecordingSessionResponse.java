package com.studioos.server.session.dto;

import java.time.LocalDateTime;

import com.studioos.server.shared.enums.SessionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordingSessionResponse {
    private String id;
    private String bookingId;
    private String studioId;
    private Integer artistId;
    private Integer producerId;
    private Integer engineerId;
    private LocalDateTime scheduledStart;
    private LocalDateTime scheduledEnd;
    private LocalDateTime actualStart;
    private LocalDateTime actualEnd;
    private SessionStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
