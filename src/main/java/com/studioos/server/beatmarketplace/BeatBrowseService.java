package com.studioos.server.beatmarketplace;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.beatmarketplace.dto.BeatSearchRequest;
import com.studioos.server.beatmarketplace.dto.BeatSummaryResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BeatBrowseService {

    private final BeatRepository beatRepository;
    private final BeatLicenseRepository beatLicenseRepository;

    @Transactional(readOnly = true)
    public Page<BeatSummaryResponse> search(BeatSearchRequest request) {

        BeatSearchCriteria criteria = BeatSearchCriteria.builder()
                .genreId(request.getGenreId())
                .mood(request.getMood())
                .bpmMin(request.getBpmMin())
                .bpmMax(request.getBpmMax())
                .keySignature(request.getKeySignature())
                .producerId(request.getProducerId())
                .studioId(request.getStudioId())
                .priceMin(request.getPriceMin())
                .priceMax(request.getPriceMax())
                .build();

        Specification<Beat> spec = Specification
                .where(BeatSpecifications.publicAndReady())
                .and(BeatSpecifications.matchesCriteria(criteria));

        Sort sort = resolveSort(request.getSortBy());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Page<Beat> beats = beatRepository.findAll(spec, pageable);

        List<String> beatIds = beats.getContent().stream().map(Beat::getId).collect(Collectors.toList());
        Map<String, Integer> minPrices = beatIds.isEmpty()
                ? Map.of()
                : beatLicenseRepository.findMinPricesByBeatIds(beatIds).stream()
                        .collect(Collectors.toMap(BeatMinPriceProjection::getBeatId, BeatMinPriceProjection::getMinPrice));

        return beats.map(beat -> toSummary(beat, minPrices.get(beat.getId())));
    }

    private Sort resolveSort(String sortBy) {
        if (sortBy == null) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sortBy.toUpperCase()) {
            case "POPULAR", "TRENDING" -> Sort.by(Sort.Direction.DESC, "playCount");
            // TODO: TRENDING currently aliases POPULAR (raw playCount).
            // A real trending algorithm needs time-decayed scoring against BeatPlayHistory,
            // not just a lifetime playCount sort.
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private BeatSummaryResponse toSummary(Beat beat, Integer startingPrice) {
        return BeatSummaryResponse.builder()
                .id(beat.getId())
                .title(beat.getTitle())
                .coverUrl(beat.getCoverUrl())
                .genreName(beat.getGenre() != null ? beat.getGenre().getName() : null)
                .startingPrice(startingPrice)
                .likeCount(beat.getLikeCount())
                .playCount(beat.getPlayCount())
                .producerId(String.valueOf(beat.getProducerId()))
                .duration(beat.getDuration())
                .waveformUrl(beat.getWaveformUrl())
                .previewAvailable(beat.getPreviewUrl() != null && !beat.getPreviewUrl().isBlank())
                .build();
    }
}