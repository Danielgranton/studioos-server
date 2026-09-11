package com.studioos.server.search.service;

import com.studioos.server.search.OpenSearchQueryClient;
import com.studioos.server.search.document.ProducerDocument;
import com.studioos.server.search.dto.ProducerSearchResult;
import com.studioos.server.search.dto.SearchPageResponse;
import com.studioos.server.search.mapper.ProducerMapper;
import com.studioos.server.search.exception.SearchException;
import com.studioos.server.search.util.SearchSanitizer;
import com.studioos.server.shared.storage.PresignedUrlService;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProducerSearchService {

    private final OpenSearchQueryClient openSearchClient;
    private final PresignedUrlService presignedUrlService;

    @Value("${storage.s3.profile-url-expiry-seconds:3600}")
    private int profileUrlExpirySeconds;

    public SearchPageResponse<ProducerSearchResult> search(String query, int page, int size) {
        String needle = SearchSanitizer.sanitize(query);
        try {
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();
            if (!needle.isBlank()) {
            boolQuery.should(s -> s.match(m -> m.field("name").query(q -> q.stringValue(needle))))
                    .should(s -> s.match(m -> m.field("genre").query(q -> q.stringValue(needle))))
                    .should(s -> s.match(m -> m.field("location").query(q -> q.stringValue(needle))))
                    .should(s -> s.match(m -> m.field("bio").query(q -> q.stringValue(needle))))
                    .should(s -> s.match(m -> m.field("studioNames").query(q -> q.stringValue(needle))))
                    .should(s -> s.match(m -> m.field("services").query(q -> q.stringValue(needle))))
                    .minimumShouldMatch("1");
            }
            Query finalQuery = Query.of(q -> q.bool(boolQuery.build()));
            SearchResponse<ProducerDocument> response = openSearchClient.search(s -> s
                        .index("producers").query(finalQuery).from(page * size).size(size),
                ProducerDocument.class);
            List<ProducerSearchResult> results = response.hits().hits().stream()
                .filter(hit -> hit.source() != null)
                .map(hit -> {
                    ProducerSearchResult result = ProducerMapper.toResult(hit.source(), hit.score());
                    result.setProfileImage(resolveImageUrl(result.getProfileImage()));
                    result.setProfileImageThumbnail(resolveImageUrl(result.getProfileImageThumbnail()));
                    return result;
                })
                .toList();
            long total = response.hits().total() == null ? results.size() : response.hits().total().value();
            return SearchPageResponse.<ProducerSearchResult>builder()
                    .results(results).page(page).size(size).total(total).build();
        } catch (Exception e) {
            throw new SearchException("Producer search is temporarily unavailable", e);
        }
    }

    private String resolveImageUrl(String reference) {
        if (reference == null || !reference.startsWith("s3://")) return reference;
        String remainder = reference.substring("s3://".length());
        int separator = remainder.indexOf('/');
        if (separator <= 0 || separator == remainder.length() - 1) return reference;
        return presignedUrlService.generateDownloadUrl(
                remainder.substring(0, separator),
                remainder.substring(separator + 1),
                profileUrlExpirySeconds);
    }
}
