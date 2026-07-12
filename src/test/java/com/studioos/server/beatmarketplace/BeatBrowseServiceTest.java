package com.studioos.server.beatmarketplace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import com.studioos.server.beatmarketplace.dto.BeatSearchRequest;
import com.studioos.server.beatmarketplace.dto.BeatSummaryResponse;
import com.studioos.server.shared.enums.BeatStatus;
import com.studioos.server.shared.enums.BeatVisibility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class BeatBrowseServiceTest {

    @Mock
    private BeatRepository beatRepository;

    @Mock
    private BeatLicenseRepository beatLicenseRepository;

    @Mock
    private BeatPlayHistoryRepository beatPlayHistoryRepository;

    @InjectMocks
    private BeatBrowseService beatBrowseService;

    @Test
    void trendingSortRanksRecentPlaysAheadOfOlderHighVolumePlays() {
        Beat recentBeat = Beat.builder()
                .id("beat-recent")
                .producerId(1)
                .title("Recent Beat")
                .status(BeatStatus.READY)
                .visibility(BeatVisibility.PUBLIC)
                .playCount(20)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();
        Beat olderBeat = Beat.builder()
                .id("beat-older")
                .producerId(2)
                .title("Older Beat")
                .status(BeatStatus.READY)
                .visibility(BeatVisibility.PUBLIC)
                .playCount(100)
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();

        when(beatRepository.findAll(any(Specification.class))).thenReturn(List.of(olderBeat, recentBeat));
        when(beatPlayHistoryRepository.findByBeatIdInAndPlayedAtAfter(anyList(), any(LocalDateTime.class)))
                .thenReturn(
                        List.of(
                                BeatPlayHistory.builder()
                                        .beatId("beat-recent")
                                        .playedAt(LocalDateTime.now().minusMinutes(10))
                                        .build(),
                                BeatPlayHistory.builder()
                                        .beatId("beat-recent")
                                        .playedAt(LocalDateTime.now().minusMinutes(20))
                                        .build(),
                                BeatPlayHistory.builder()
                                        .beatId("beat-recent")
                                        .playedAt(LocalDateTime.now().minusMinutes(30))
                                        .build(),
                                BeatPlayHistory.builder()
                                        .beatId("beat-recent")
                                        .playedAt(LocalDateTime.now().minusMinutes(40))
                                        .build(),
                                BeatPlayHistory.builder()
                                        .beatId("beat-recent")
                                        .playedAt(LocalDateTime.now().minusMinutes(50))
                                        .build(),
                                BeatPlayHistory.builder()
                                        .beatId("beat-older")
                                        .playedAt(LocalDateTime.now().minusDays(20))
                                        .build(),
                                BeatPlayHistory.builder()
                                        .beatId("beat-older")
                                        .playedAt(LocalDateTime.now().minusDays(25))
                                        .build(),
                                BeatPlayHistory.builder()
                                        .beatId("beat-older")
                                        .playedAt(LocalDateTime.now().minusDays(28))
                                        .build()));
        when(beatLicenseRepository.findMinPricesByBeatIds(anyList())).thenReturn(List.<BeatMinPriceProjection>of());

        BeatSearchRequest request = new BeatSearchRequest();
        request.setSortBy("TRENDING");
        request.setPage(0);
        request.setSize(10);

        List<BeatSummaryResponse> content = beatBrowseService.search(request).getContent();

        assertThat(content).hasSize(2);
        assertThat(content.get(0).getId()).isEqualTo("beat-recent");
        assertThat(content.get(1).getId()).isEqualTo("beat-older");
    }
}
