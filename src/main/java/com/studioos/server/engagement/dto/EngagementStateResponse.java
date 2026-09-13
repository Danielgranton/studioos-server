package com.studioos.server.engagement.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EngagementStateResponse {
    boolean active;
    long followerCount;
    long favoriteCount;
    long viewCount;
}
