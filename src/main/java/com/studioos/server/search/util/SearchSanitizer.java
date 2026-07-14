package com.studioos.server.search.util;

public final class SearchSanitizer {
    private SearchSanitizer() {
    }

    public static String sanitize(String query) {
        return QueryNormalizer.normalize(query).replaceAll("[^\\p{L}\\p{N}\\s-]", "");
    }
}
