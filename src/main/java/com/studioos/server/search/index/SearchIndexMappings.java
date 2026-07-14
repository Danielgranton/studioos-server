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
                        .properties("pricing", p -> p.integer(i -> i))
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
                        .properties("averageRating", p -> p.double_(d -> d))
                        .properties("reviewCount", p -> p.integer(i -> i))
                        .properties("createdAt", p -> p.date(d -> d))
                ));
    }
}
