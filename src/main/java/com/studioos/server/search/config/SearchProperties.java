package com.studioos.server.search.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "search")
public class SearchProperties {

    private final Index index = new Index();
    private final Cache cache = new Cache();

    @Data
    public static class Index {
        private String beats = "beats";
        private String studios = "studios";
        private String producers = "producers";
        private String advertisements = "advertisements";
    }

    @Data
    public static class Cache {
        private Duration ttl = Duration.ofMinutes(5);
        private int recentLimit = 10;
        private int trendingLimit = 20;
    }
}
