package com.studioos.server.search.scheduler;

import com.studioos.server.search.index.SearchIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchMaintenanceScheduler {

    private final SearchIndexService searchIndexService;

    @Scheduled(cron = "0 0 * * * *")
    public void refreshTrending() {
        log.debug("Search trending refresh tick");
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void reindexNightly() {
        searchIndexService.reindexAll();
    }
}
