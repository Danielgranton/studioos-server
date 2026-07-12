package com.studioos.server.beatmarketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BeatSummaryResponse {
    private String id;
    private String title;
    private String coverUrl;
    private String thumbnailUrl;
    private String genreName;
    private Integer startingPrice;   // cheapest active license price, null if no licenses yet
    private Integer likeCount;
    private Integer playCount;
    private String producerId;       // TODO: add producerName once User's display-name field is confirmed
    private Integer duration;
    private String waveformUrl;
    private boolean previewAvailable;
}
