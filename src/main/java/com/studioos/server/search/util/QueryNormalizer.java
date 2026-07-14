package com.studioos.server.search.util;

import java.text.Normalizer;

public final class QueryNormalizer {
    private QueryNormalizer() {
    }

    public static String normalize(String query) {
        if (query == null) {
            return "";
        }
        String normalized = Normalizer.normalize(query, Normalizer.Form.NFKC);
        return normalized.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
