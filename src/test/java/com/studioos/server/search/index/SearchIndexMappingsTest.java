package com.studioos.server.search.index;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SearchIndexMappingsTest {

    @Test
    void beatIndexUsesExplicitFieldMappings() {
        var request = SearchIndexMappings.beatIndexRequest("beats");

        assertThat(request.index()).isEqualTo("beats");
        assertThat(request.mappings()).isNotNull();
        assertThat(request.mappings().properties()).containsKeys(
                "title", "genre", "keySignature", "mood", "producerId", "studioId",
                "price", "playCount", "likeCount", "status", "createdAt");
        assertThat(request.mappings().properties().get("title").isText()).isTrue();
        assertThat(request.mappings().properties().get("genre").isKeyword()).isTrue();
        assertThat(request.mappings().properties().get("producerId").isInteger()).isTrue();
        assertThat(request.mappings().properties().get("createdAt").isDate()).isTrue();
    }

    @Test
    void studioIndexUsesExplicitFieldMappings() {
        var request = SearchIndexMappings.studioIndexRequest("studios");

        assertThat(request.index()).isEqualTo("studios");
        assertThat(request.mappings()).isNotNull();
        assertThat(request.mappings().properties()).containsKeys(
                "studioName", "location", "description", "pricing", "ownerId", "averageRating", "ratingCount");
        assertThat(request.mappings().properties().get("studioName").isText()).isTrue();
        assertThat(request.mappings().properties().get("location").isKeyword()).isTrue();
        assertThat(request.mappings().properties().get("averageRating").isDouble()).isTrue();
        assertThat(request.mappings().properties().get("ratingCount").isInteger()).isTrue();
    }
}
