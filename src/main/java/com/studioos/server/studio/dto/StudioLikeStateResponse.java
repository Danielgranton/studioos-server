package com.studioos.server.studio.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StudioLikeStateResponse {
    boolean liked;
    long likeCount;
}
