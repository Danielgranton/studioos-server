package com.studioos.server.notification.dto;

import java.time.LocalDateTime;

import com.studioos.server.shared.enums.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private String id;
    private NotificationType type;
    private String title;
    private String message;
    private String relatedEntityId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}