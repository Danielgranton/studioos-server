package com.studioos.server.user.dto;

import lombok.Data;

@Data
public class UpdatePrivacySettingsRequest {
    private Boolean profileDiscoverable;
    private Boolean emailVisible;
    private Boolean phoneVisible;
    private Boolean directMessagesEnabled;
    private Boolean personalizedRecommendations;
}
