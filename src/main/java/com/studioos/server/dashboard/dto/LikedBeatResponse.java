package com.studioos.server.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class LikedBeatResponse {

    private String beatId;
    private String title;
    private String coverUrl;
    private Integer producerId;
}