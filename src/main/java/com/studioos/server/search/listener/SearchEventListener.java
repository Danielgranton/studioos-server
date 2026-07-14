package com.studioos.server.search.listener;

import com.studioos.server.search.event.AdvertisementPublishedEvent;
import com.studioos.server.search.event.BeatCreatedEvent;
import com.studioos.server.search.event.BeatDeletedEvent;
import com.studioos.server.search.event.BeatUpdatedEvent;
import com.studioos.server.search.event.PurchaseCompletedEvent;
import com.studioos.server.search.event.ReviewCreatedEvent;
import com.studioos.server.search.event.StudioCreatedEvent;
import com.studioos.server.search.event.StudioDeletedEvent;
import com.studioos.server.search.event.StudioUpdatedEvent;
import com.studioos.server.search.index.SearchIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchEventListener {

    private final SearchIndexService searchIndexService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBeatCreated(BeatCreatedEvent event) {
        log.debug("Search event received for beat created {}", event.getBeatId());
        searchIndexService.reindexAll();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBeatUpdated(BeatUpdatedEvent event) {
        log.debug("Search event received for beat updated {}", event.getBeatId());
        searchIndexService.reindexAll();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBeatDeleted(BeatDeletedEvent event) {
        log.debug("Search event received for beat deleted {}", event.getBeatId());
        searchIndexService.reindexAll();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStudioCreated(StudioCreatedEvent event) {
        log.debug("Search event received for studio created {}", event.getStudioId());
        searchIndexService.reindexAll();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStudioUpdated(StudioUpdatedEvent event) {
        log.debug("Search event received for studio updated {}", event.getStudioId());
        searchIndexService.reindexAll();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStudioDeleted(StudioDeletedEvent event) {
        log.debug("Search event received for studio deleted {}", event.getStudioId());
        searchIndexService.reindexAll();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAdPublished(AdvertisementPublishedEvent event) {
        log.debug("Search event received for ad published {}", event.getAdvertisementId());
        searchIndexService.reindexAll();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewCreated(ReviewCreatedEvent event) {
        log.debug("Search event received for review created {} {}", event.getEntityType(), event.getEntityId());
        searchIndexService.reindexAll();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPurchaseCompleted(PurchaseCompletedEvent event) {
        log.debug("Search event received for purchase completed {}", event.getEntityId());
        searchIndexService.reindexAll();
    }
}
