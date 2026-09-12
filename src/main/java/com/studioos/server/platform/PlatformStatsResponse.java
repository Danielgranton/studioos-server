package com.studioos.server.platform;

public record PlatformStatsResponse(
        long studios,
        long producers,
        long beats,
        long artists,
        long services,
        long campaigns
) {
}
