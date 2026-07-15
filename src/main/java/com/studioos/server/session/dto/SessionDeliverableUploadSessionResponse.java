package com.studioos.server.session.dto;

import java.time.LocalDateTime;

import com.studioos.server.shared.enums.DeliverableStatus;
import com.studioos.server.shared.enums.DeliverableType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDeliverableUploadSessionResponse {
    private String deliverableId;
    private String uploadUrl;
    private DeliverableStatus status;
    private DeliverableType type;
    private LocalDateTime expiresAt;
}
