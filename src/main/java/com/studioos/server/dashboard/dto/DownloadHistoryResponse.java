package com.studioos.server.dashboard.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class DownloadHistoryResponse {
    private String beatId;
    private String title;
    private LocalDateTime downloadedAt;
}