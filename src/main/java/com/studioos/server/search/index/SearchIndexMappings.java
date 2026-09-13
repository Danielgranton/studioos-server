package com.studioos.server.search.index;

import org.opensearch.client.opensearch.indices.CreateIndexRequest;

final class SearchIndexMappings {

    private SearchIndexMappings() {
    }

    static CreateIndexRequest createIndexRequest(String indexName) {
        return switch (indexName) {
            case "beats" -> beatIndexRequest(indexName);
            case "studios" -> studioIndexRequest(indexName);
            case "advertisements" -> advertisementIndexRequest(indexName);
            case "producers" -> producerIndexRequest(indexName);
            default -> CreateIndexRequest.of(c -> c.index(indexName));
        };
    }

    static CreateIndexRequest beatIndexRequest(String indexName) {
        return CreateIndexRequest.of(c -> c
                .index(indexName)
                .mappings(m -> m
                        .properties("title", p -> p.text(t -> t))
                        .properties("genre", p -> p.keyword(k -> k))
                        .properties("keySignature", p -> p.keyword(k -> k))
                        .properties("mood", p -> p.keyword(k -> k))
                        .properties("producerId", p -> p.integer(i -> i))
                        .properties("studioId", p -> p.keyword(k -> k))
                        .properties("price", p -> p.integer(i -> i))
                        .properties("playCount", p -> p.integer(i -> i))
                        .properties("likeCount", p -> p.integer(i -> i))
                        .properties("status", p -> p.keyword(k -> k))
                        .properties("createdAt", p -> p.date(d -> d))
                ));
    }

    static CreateIndexRequest studioIndexRequest(String indexName) {
        return CreateIndexRequest.of(c -> c
                .index(indexName)
                .mappings(m -> m
                        .properties("studioName", p -> p.text(t -> t))
                        .properties("location", p -> p.keyword(k -> k))
                        .properties("description", p -> p.text(t -> t))
                        .properties("badge", p -> p.keyword(k -> k))
                        .properties("genres", p -> p.keyword(k -> k))
                        .properties("equipment", p -> p.keyword(k -> k))
                        .properties("rooms", p -> p.integer(i -> i))
                        .properties("yearsActive", p -> p.integer(i -> i))
                        .properties("responseTime", p -> p.keyword(k -> k))
                        .properties("available", p -> p.boolean_(b -> b))
                        .properties("nextAvailable", p -> p.keyword(k -> k))
                        .properties("bookings", p -> p.integer(i -> i))
                        .properties("verified", p -> p.boolean_(b -> b))
                        .properties("pricing", p -> p.integer(i -> i))
                        .properties("profileImageThumbnail", p -> p.keyword(k -> k))
                        .properties("verified", p -> p.boolean_(b -> b))
                        .properties("ownerId", p -> p.integer(i -> i))
                        .properties("averageRating", p -> p.double_(d -> d))
                        .properties("ratingCount", p -> p.integer(i -> i))
                ));
    }

    static CreateIndexRequest advertisementIndexRequest(String indexName) {
        return CreateIndexRequest.of(c -> c
                .index(indexName)
                .mappings(m -> m
                        .properties("campaignId", p -> p.keyword(k -> k))
                        .properties("advertiserId", p -> p.integer(i -> i))
                        .properties("campaignTitle", p -> p.text(t -> t))
                        .properties("type", p -> p.keyword(k -> k))
                        .properties("headline", p -> p.text(t -> t))
                        .properties("description", p -> p.text(t -> t))
                        .properties("ctaText", p -> p.text(t -> t))
                        .properties("ctaUrl", p -> p.keyword(k -> k))
                        .properties("mediaUrl", p -> p.keyword(k -> k))
                        .properties("thumbnailUrl", p -> p.keyword(k -> k))
                        .properties("duration", p -> p.integer(i -> i))
                        .properties("status", p -> p.keyword(k -> k))
                        .properties("createdAt", p -> p.date(d -> d))
                ));
    }

    static CreateIndexRequest producerIndexRequest(String indexName) {
        return CreateIndexRequest.of(c -> c
                .index(indexName)
                .mappings(m -> m
                        .properties("name", p -> p.text(t -> t))
                        .properties("location", p -> p.keyword(k -> k))
                        .properties("genre", p -> p.keyword(k -> k))
                        .properties("bio", p -> p.text(t -> t))
                        .properties("profileImage", p -> p.keyword(k -> k))
                        .properties("profileImageThumbnail", p -> p.keyword(k -> k))
                        .properties("studioNames", p -> p.keyword(k -> k))
                        .properties("studioCount", p -> p.integer(i -> i))
                        .properties("available", p -> p.boolean_(b -> b))
                        .properties("startingPrice", p -> p.integer(i -> i))
                        .properties("responseTime", p -> p.keyword(k -> k))
                        .properties("services", p -> p.keyword(k -> k))
                        .properties("averageRating", p -> p.double_(d -> d))
                        .properties("reviewCount", p -> p.integer(i -> i))
                        .properties("followerCount", p -> p.long_(l -> l))
                        .properties("beatCount", p -> p.long_(l -> l))
                        .properties("popularityScore", p -> p.double_(d -> d))
                        .properties("trendingScore", p -> p.double_(d -> d))
                        .properties("featured", p -> p.boolean_(b -> b))
                        .properties("createdAt", p -> p.date(d -> d))
                ));
    }
}
