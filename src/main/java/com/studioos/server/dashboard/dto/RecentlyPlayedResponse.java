package com.studioos.server.dashboard.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class RecentlyPlayedResponse {
    private String beatId;
    private String title;
    private String coverUrl;
    private LocalDateTime playedAt;
}