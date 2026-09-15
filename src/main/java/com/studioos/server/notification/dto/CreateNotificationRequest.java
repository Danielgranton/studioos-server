package com.studioos.server.notification.dto;

import com.studioos.server.shared.enums.NotificationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateNotificationRequest {
    @NotNull
    private Integer userId;

    @NotNull
    private NotificationType type;

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    @Size(max = 10000)
    private String message;

    @Size(max = 36)
    private String relatedEntityId;
}
