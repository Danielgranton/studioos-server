package com.studioos.server.search.index;

import com.studioos.server.advertisement.AdvertisementRepository;
import com.studioos.server.advertisement.campaign.AdCampaignRepository;
import com.studioos.server.beatmarketplace.BeatRepository;
import com.studioos.server.studio.StudioRepository;
import com.studioos.server.user.UserRepository;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchIndexService {

    private final BeatSearchIndexingService beatSearchIndexingService;
    private final StudioSearchIndexingService studioSearchIndexingService;
    private final AdvertisementSearchIndexingService advertisementSearchIndexingService;
    private final ProducerSearchIndexingService producerSearchIndexingService;
    private final BeatRepository beatRepository;
    private final StudioRepository studioRepository;
    private final AdvertisementRepository advertisementRepository;
    private final AdCampaignRepository adCampaignRepository;
    private final UserRepository userRepository;

    public void reindexAll() {
        AtomicInteger count = new AtomicInteger();
        beatRepository.findAll().forEach(beat -> {
            beatSearchIndexingService.indexBeat(beat);
            count.incrementAndGet();
        });
        studioRepository.findAll().forEach(studio -> {
            studioSearchIndexingService.indexStudio(studio);
            count.incrementAndGet();
        });
        advertisementRepository.findAll().forEach(ad -> {
            adCampaignRepository.findById(ad.getCampaignId())
                    .ifPresent(campaign -> advertisementSearchIndexingService.indexAdvertisement(ad, campaign));
            count.incrementAndGet();
        });
        userRepository.findByRole(com.studioos.server.shared.enums.Role.PRODUCER).forEach(producer -> {
            producerSearchIndexingService.indexProducer(producer);
            count.incrementAndGet();
        });
        log.info("Search reindex completed for {} entities", count.get());
    }
}
