package com.studioos.server.platform;

import java.util.List;

public record FeaturedCreatorsResponse(
        List<FeaturedCreatorResponse> creators,
        long creatorCount,
        Double averageRating
) {
}
