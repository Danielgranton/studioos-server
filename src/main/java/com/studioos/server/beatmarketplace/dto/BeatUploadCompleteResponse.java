package com.studioos.server.beatmarketplace.dto;

import com.studioos.server.shared.enums.BeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
@AllArgsConstructor
public class BeatUploadCompleteResponse {
    private String beatId;
    private BeatStatus status;
}
