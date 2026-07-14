package com.studioos.server.search.service;

import com.studioos.server.reviews.ProducerReviewRepository;
import com.studioos.server.search.dto.ProducerSearchResult;
import com.studioos.server.search.mapper.ProducerMapper;
import com.studioos.server.search.util.SearchSanitizer;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.user.UserRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProducerSearchService {

    private final UserRepository userRepository;
    private final ProducerReviewRepository producerReviewRepository;

    public List<ProducerSearchResult> search(String query, int page, int size) {
        String needle = SearchSanitizer.sanitize(query);
        return userRepository.findByRole(Role.PRODUCER).stream()
                .filter(user -> matches(user.getName(), needle)
                        || matches(user.getGenre(), needle)
                        || matches(user.getLocation(), needle)
                        || matches(user.getBio(), needle))
                .map(user -> ProducerMapper.toDocument(
                        user,
                        producerReviewRepository.findAverageRatingByProducerId(user.getId()),
                        (int) producerReviewRepository.countByProducerId(user.getId())))
                .map(doc -> ProducerMapper.toResult(doc, score(doc, needle)))
                .sorted(Comparator.comparingDouble(ProducerSearchResult::getScore).reversed())
                .skip((long) page * size)
                .limit(size)
                .toList();
    }

    private boolean matches(String value, String needle) {
        return needle.isBlank() || (value != null && value.toLowerCase().contains(needle));
    }

    private double score(com.studioos.server.search.document.ProducerDocument doc, String needle) {
        double nameBoost = matches(doc.getName(), needle) ? 1.0 : 0.0;
        double genreBoost = matches(doc.getGenre(), needle) ? 0.6 : 0.0;
        double bioBoost = matches(doc.getBio(), needle) ? 0.3 : 0.0;
        double ratingBoost = doc.getAverageRating() != null ? doc.getAverageRating() / 5.0 : 0.0;
        return nameBoost + genreBoost + bioBoost + ratingBoost;
    }
}
