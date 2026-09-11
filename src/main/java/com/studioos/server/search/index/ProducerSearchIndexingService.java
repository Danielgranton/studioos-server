package com.studioos.server.search.index;

import com.studioos.server.reviews.ProducerReviewRepository;
import com.studioos.server.search.document.ProducerDocument;
import com.studioos.server.search.mapper.ProducerMapper;
import com.studioos.server.user.User;
import com.studioos.server.studio.Studio;
import com.studioos.server.studio.StudioRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProducerSearchIndexingService {

    private static final String INDEX_NAME = "producers";

    private final OpenSearchClient openSearchClient;
    private final ProducerReviewRepository producerReviewRepository;
    private final StudioRepository studioRepository;

    public void indexProducer(User producer) {
        try {
            Double averageRating = producerReviewRepository.findAverageRatingByProducerId(producer.getId());
            double avg = averageRating != null ? averageRating : 0.0;
            int count = (int) producerReviewRepository.countByProducerId(producer.getId());
            List<Studio> studios = studioRepository.findByOwnerId(producer.getId());
            List<String> studioNames = studios.stream()
                    .map(Studio::getStudioName)
                    .filter(Objects::nonNull)
                    .toList();
            List<String> services = studios.stream()
                    .flatMap(studio -> studio.getServices().stream())
                    .map(service -> service.getName())
                    .filter(Objects::nonNull)
                    .distinct()
                    .limit(6)
                    .toList();
            Integer startingPrice = studios.stream()
                    .map(Studio::getPricing)
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(null);
            String responseTime = studios.stream()
                    .map(Studio::getResponseTime)
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst()
                    .orElse(null);
            boolean available = studios.stream().anyMatch(Studio::isAvailable);

            ProducerDocument doc = ProducerMapper.toDocument(producer, avg, count);
            doc.setProfileImageThumbnail(producer.getProfileImageThumbnail());
            doc.setVerified(producer.isAccountVerified());
            doc.setStudioNames(studioNames);
            doc.setStudioCount(studios.size());
            doc.setAvailable(!studios.isEmpty() && available);
            doc.setStartingPrice(startingPrice);
            doc.setResponseTime(responseTime);
            doc.setServices(services);
            openSearchClient.index(i -> i.index(INDEX_NAME).id(String.valueOf(producer.getId())).document(doc));
        } catch (Exception e) {
            log.error("Failed to index producer {} in OpenSearch: {}", producer.getId(), e.getMessage());
        }
    }
}
