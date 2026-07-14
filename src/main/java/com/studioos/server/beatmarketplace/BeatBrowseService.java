package com.studioos.server.beatmarketplace;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.beatmarketplace.dto.BeatSearchRequest;
import com.studioos.server.beatmarketplace.dto.BeatSummaryResponse;
import com.studioos.server.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BeatBrowseService {

    private final BeatRepository beatRepository;
    private final BeatLicenseRepository beatLicenseRepository;
    private final BeatPlayHistoryRepository beatPlayHistoryRepository;
    private final UserRepository userRepository;

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

        Specification<Beat> spec = BeatSpecifications.publicAndReady()
                .and(BeatSpecifications.matchesCriteria(criteria));

        if ("TRENDING".equalsIgnoreCase(request.getSortBy())) {
            return searchTrending(spec, request.getPage(), request.getSize());
        }

        Sort sort = resolveSort(request.getSortBy());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        Page<Beat> beats = beatRepository.findAll(spec, pageable);

        List<String> beatIds = beats.getContent().stream().map(b -> b.getId()).collect(Collectors.toList());
        Map<String, Integer> minPrices = beatIds.isEmpty()
                ? Map.of()
                : beatLicenseRepository.findMinPricesByBeatIds(beatIds).stream()
                        .collect(Collectors.toMap(b -> b.getBeatId(), b -> b.getMinPrice()));

        Map<Integer, String> producerNames = resolveProducerNames(beats.getContent());

        return beats.map(beat -> toSummary(beat, minPrices.get(beat.getId()), producerNames.get(beat.getProducerId())));
    }

    private Sort resolveSort(String sortBy) {
        if (sortBy == null) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sortBy.toUpperCase()) {
            case "POPULAR" -> Sort.by(Sort.Direction.DESC, "playCount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private Page<BeatSummaryResponse> searchTrending(Specification<Beat> spec, int page, int size) {
        List<Beat> beats = beatRepository.findAll(spec);
        if (beats.isEmpty()) {
            return Page.empty(PageRequest.of(page, size));
        }

        List<String> beatIds = beats.stream().map(b -> b.getId()).toList();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        Map<String, Double> trendingScores = computeTrendingScores(beatIds, cutoff);

        List<Beat> sorted = new ArrayList<>(beats);
        sorted.sort(Comparator
                .comparingDouble((Beat beat) -> trendingScores.getOrDefault(beat.getId(), 0.0)).reversed()
                .thenComparing(b -> b.getPlayCount(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(b -> b.getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder())));

        int fromIndex = Math.min(page * size, sorted.size());
        int toIndex = Math.min(fromIndex + size, sorted.size());
        List<Beat> content = sorted.subList(fromIndex, toIndex);

        List<String> pageBeatIds = content.stream().map(b -> b.getId()).collect(Collectors.toList());
        Map<String, Integer> minPrices = pageBeatIds.isEmpty()
                ? Map.of()
                : beatLicenseRepository.findMinPricesByBeatIds(pageBeatIds).stream()
                        .collect(Collectors.toMap(b -> b.getBeatId(), b -> b.getMinPrice()));

        Map<Integer, String> producerNames = resolveProducerNames(content);

        return new PageImpl<>(
                content.stream().map(beat -> toSummary(beat, minPrices.get(beat.getId()), producerNames.get(beat.getProducerId()))).toList(),
                PageRequest.of(page, size),
                sorted.size());
    }

    private Map<String, Double> computeTrendingScores(List<String> beatIds, LocalDateTime cutoff) {
        Map<String, Double> scores = new HashMap<>();
        List<BeatPlayHistory> recentPlays = beatPlayHistoryRepository.findByBeatIdInAndPlayedAtAfter(beatIds, cutoff);

        LocalDateTime now = LocalDateTime.now();
        for (BeatPlayHistory play : recentPlays) {
            long hoursOld = Math.max(0, ChronoUnit.HOURS.between(play.getPlayedAt(), now));
            double recencyWeight = Math.exp(-(hoursOld / 168.0));
            scores.merge(play.getBeatId(), recencyWeight, Double::sum);
        }

        return scores;
    }

    private Map<Integer, String> resolveProducerNames(List<Beat> beats) {
        if (userRepository == null) {
            return Map.of();
        }

        Set<Integer> producerIds = beats.stream()
                .map(b ->b.getProducerId())
                .collect(Collectors.toSet());
        return StreamSupport.stream(userRepository.findAllById(producerIds).spliterator(), false)
                .collect(Collectors.toMap(
                        u -> u.getId(),
                        u -> u.getName(),
                        (left, right) -> left));
    }

    private BeatSummaryResponse toSummary(Beat beat, Integer startingPrice, String producerName) {
        return BeatSummaryResponse.builder()
                .id(beat.getId())
                .title(beat.getTitle())
                .coverUrl(beat.getCoverUrl())
                .thumbnailUrl(beat.getThumbnailUrl())
                .genreName(beat.getGenre() != null ? beat.getGenre().getName() : null)
                .startingPrice(startingPrice)
                .likeCount(beat.getLikeCount())
                .playCount(beat.getPlayCount())
                .producerId(String.valueOf(beat.getProducerId()))
                .producerName(producerName)
                .duration(beat.getDuration())
                .waveformUrl(beat.getWaveformUrl())
                .previewAvailable(beat.getPreviewUrl() != null && !beat.getPreviewUrl().isBlank())
                .build();
    }
}
