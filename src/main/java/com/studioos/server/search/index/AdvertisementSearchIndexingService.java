package com.studioos.server.search.index;

import com.studioos.server.advertisement.Advertisement;
import com.studioos.server.advertisement.campaign.AdCampaign;
import com.studioos.server.search.document.AdvertisementDocument;
import com.studioos.server.search.mapper.AdvertisementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdvertisementSearchIndexingService {

    private static final String INDEX_NAME = "advertisements";

    private final OpenSearchClient openSearchClient;

    public void indexAdvertisement(Advertisement advertisement, AdCampaign campaign) {
        try {
            AdvertisementDocument doc = AdvertisementMapper.toDocument(advertisement, campaign);
            openSearchClient.index(i -> i.index(INDEX_NAME).id(advertisement.getId()).document(doc));
        } catch (Exception e) {
            log.error("Failed to index advertisement {} in OpenSearch: {}", advertisement.getId(), e.getMessage());
        }
    }
}
