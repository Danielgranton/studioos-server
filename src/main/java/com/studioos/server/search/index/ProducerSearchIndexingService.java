package com.studioos.server.search.index;

import com.studioos.server.reviews.ProducerReviewRepository;
import com.studioos.server.search.document.ProducerDocument;
import com.studioos.server.search.mapper.ProducerMapper;
import com.studioos.server.user.User;
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

    public void indexProducer(User producer) {
        try {
            double avg = producerReviewRepository.findAverageRatingByProducerId(producer.getId());
            int count = (int) producerReviewRepository.countByProducerId(producer.getId());
            ProducerDocument doc = ProducerMapper.toDocument(producer, avg, count);
            openSearchClient.index(i -> i.index(INDEX_NAME).id(String.valueOf(producer.getId())).document(doc));
        } catch (Exception e) {
            log.error("Failed to index producer {} in OpenSearch: {}", producer.getId(), e.getMessage());
        }
    }
}
