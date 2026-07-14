package com.studioos.server.search.ranking;

import com.studioos.server.search.document.BeatDocument;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class BeatRankingEngine {

    // Weights sum to 1.0 — adjust here, not scattered through calculation logic.
    private static final double TEXT_MATCH_WEIGHT = 0.55;
    private static final double POPULARITY_WEIGHT = 0.25;
    private static final double RECENCY_WEIGHT = 0.20;

    public double score(Hit<BeatDocument> hit, double maxTextScore, int maxPlayCount) {
        BeatDocument doc = hit.source();
        if (doc == null) return 0.0;

        double textMatchNorm = maxTextScore > 0 ? (hit.score() != null ? hit.score() : 0) / maxTextScore : 0;
        double popularityNorm = maxPlayCount > 0 && doc.getPlayCount() != null
                ? (double) doc.getPlayCount() / maxPlayCount : 0;
        double recencyNorm = calculateRecencyScore(doc.getCreatedAt());

        return (textMatchNorm * TEXT_MATCH_WEIGHT)
                + (popularityNorm * POPULARITY_WEIGHT)
                + (recencyNorm * RECENCY_WEIGHT);
    }

    private double calculateRecencyScore(String createdAtIso) {
        if (createdAtIso == null) return 0.0;
        try {
            LocalDateTime createdAt = LocalDateTime.parse(createdAtIso);
            long daysOld = Duration.between(createdAt, LocalDateTime.now()).toDays();

            return Math.max(0.0, 1.0 - (daysOld / 90.0));
        } catch (Exception e) {
            return 0.0;
        }
    }
}