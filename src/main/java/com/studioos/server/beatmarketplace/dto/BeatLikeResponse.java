package com.studioos.server.beatmarketplace.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BeatLikeResponse {
    boolean liked;
    long likeCount;
}
