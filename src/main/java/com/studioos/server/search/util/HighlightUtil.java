package com.studioos.server.search.util;

public final class HighlightUtil {
    private HighlightUtil() {
    }

    public static String highlight(String text, String query) {
        if (text == null || query == null || query.isBlank()) {
            return text;
        }
        return text;
    }
}
