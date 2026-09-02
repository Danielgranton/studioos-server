package com.studioos.server.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivacySettingsResponse {
    private boolean profileDiscoverable;
    private boolean emailVisible;
    private boolean phoneVisible;
    private boolean directMessagesEnabled;
    private boolean personalizedRecommendations;
}
