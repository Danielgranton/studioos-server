package com.studioos.server.notification.dto;

import com.studioos.server.shared.enums.NotificationType;

import lombok.Data;

@Data
public class CreateNotificationRequest {
    private Integer userId;
    private NotificationType type;
    private String title;
    private String message;
    private String relatedEntityId;
}