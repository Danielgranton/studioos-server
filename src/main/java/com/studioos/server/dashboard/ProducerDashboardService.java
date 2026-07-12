package com.studioos.server.dashboard;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.beatmarketplace.Beat;
import com.studioos.server.beatmarketplace.BeatPurchase;
import com.studioos.server.beatmarketplace.BeatPurchaseRepository;
import com.studioos.server.beatmarketplace.BeatRepository;
import com.studioos.server.beatmarketplace.BeatReview;
import com.studioos.server.beatmarketplace.BeatReviewRepository;
import com.studioos.server.booking.Booking;
import com.studioos.server.booking.BookingRepository;
import com.studioos.server.dashboard.dto.BeatPerformanceResponse;
import com.studioos.server.dashboard.dto.ProducerDashboardResponse;
import com.studioos.server.reviews.ProducerReview;
import com.studioos.server.reviews.ProducerReviewRepository;
import com.studioos.server.payment.Wallet;
import com.studioos.server.payment.WalletRepository;
import com.studioos.server.shared.enums.BeatPaymentStatus;
import com.studioos.server.shared.enums.BeatStatus;
import com.studioos.server.shared.enums.BookingPaymentStatus;
import com.studioos.server.shared.enums.BookingStatus;
import com.studioos.server.studio.Studio;
import com.studioos.server.studio.StudioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProducerDashboardService {

    private final BeatRepository beatRepository;
    private final BeatPurchaseRepository beatPurchaseRepository;
    private final BeatReviewRepository beatReviewRepository;
    private final ProducerReviewRepository producerReviewRepository;
    private final BookingRepository bookingRepository;
    private final StudioRepository studioRepository;
    private final WalletRepository walletRepository;

    @Transactional(readOnly = true)
    public ProducerDashboardResponse getDashboard(Integer producerId) {

        LocalDateTime monthStart = LocalDateTime.now()
                .with(TemporalAdjusters.firstDayOfMonth())
                .toLocalDate().atStartOfDay();

        // ─── Beat marketplace ───
        List<Beat> beats = beatRepository.findByProducerId(producerId);
        List<String> beatIds = beats.stream().map(Beat::getId).toList();

        long totalBeats = beats.size();
        long publishedBeats = beats.stream().filter(b -> b.getStatus() == BeatStatus.READY).count();
        long draftBeats = beats.stream().filter(b -> b.getStatus() == BeatStatus.UPLOADING).count();
        long pendingProcessing = beats.stream().filter(b -> b.getStatus() == BeatStatus.PROCESSING).count();

        long totalPlays = beats.stream().mapToLong(b -> nullToZero(b.getPlayCount())).sum();
        long totalDownloads = beats.stream().mapToLong(b -> nullToZero(b.getDownloadCount())).sum();

        List<BeatPurchase> paidPurchases = beatIds.isEmpty()
                ? List.of()
                : beatPurchaseRepository.findByBeatIdInAndStatus(beatIds, BeatPaymentStatus.PAID);

        long totalSales = paidPurchases.size();
        int beatRevenue = paidPurchases.stream().mapToInt(BeatPurchase::getAmount).sum();
        int monthlyBeatRevenue = paidPurchases.stream()
                .filter(p -> p.getPurchasedAt() != null && !p.getPurchasedAt().isBefore(monthStart))
                .mapToInt(BeatPurchase::getAmount)
                .sum();

        List<BeatReview> reviews = beatIds.isEmpty() ? List.of() : beatReviewRepository.findByBeatIdIn(beatIds);
        List<ProducerReview> producerReviews = producerReviewRepository.findByProducerId(producerId);
        Double averageRating = producerReviews.isEmpty()
                ? null
                : producerReviews.stream().mapToDouble(ProducerReview::getRating).average().orElse(0.0);

        Double conversionRate = totalPlays == 0 ? null : (double) totalSales / totalPlays;

        Optional<Beat> topBeat = beats.stream()
                .max(Comparator.comparingInt(b -> nullToZero(b.getPlayCount())));

        Map<String, List<BeatPurchase>> purchasesByBeat = paidPurchases.stream()
                .collect(Collectors.groupingBy(BeatPurchase::getBeatId));
        Map<String, List<BeatReview>> reviewsByBeat = reviews.stream()
                .collect(Collectors.groupingBy(BeatReview::getBeatId));

        List<BeatPerformanceResponse> beatBreakdown = beats.stream()
                .map(beat -> toBeatPerformance(beat, purchasesByBeat, reviewsByBeat))
                .toList();

        // ─── Studio bookings ───
        List<Studio> studios = studioRepository.findByOwnerId(producerId);
        List<String> studioIds = studios.stream().map(Studio::getId).toList();

        List<Booking> allBookings = studioIds.isEmpty()
                ? List.of()
                : bookingRepository.findByStudioIdIn(studioIds);

        long totalBookings = allBookings.size();

        long upcomingBookings = allBookings.stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .filter(b -> b.getSessionDate() != null && b.getSessionDate().isAfter(LocalDateTime.now()))
                .count();

        long pendingBookingRequests = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING)
                .count();

        List<Booking> paidBookings = studioIds.isEmpty()
                ? List.of()
                : bookingRepository.findByStudioIdInAndPaymentStatus(studioIds, BookingPaymentStatus.PAID);

        int bookingRevenue = paidBookings.stream().mapToInt(b -> nullToZero(b.getTotalPrice())).sum();

        int monthlyBookingRevenue = paidBookings.stream()
                .filter(b -> b.getUpdatedAt() != null && !b.getUpdatedAt().isBefore(monthStart))
                .mapToInt(b -> nullToZero(b.getTotalPrice()))
                .sum();

        // ─── Wallet — summed across every studio the producer owns, not just one ───
        Integer walletBalance = studioIds.isEmpty()
                ? null
                : studioIds.stream()
                        .map(walletRepository::findByStudioId)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .mapToInt(w -> nullToZero(w.getAvailableBalance()))
                        .sum();

        return ProducerDashboardResponse.builder()
                .totalBeats(totalBeats)
                .publishedBeats(publishedBeats)
                .draftBeats(draftBeats)
                .pendingProcessing(pendingProcessing)
                .totalPlays(totalPlays)
                .totalDownloads(totalDownloads)
                .totalSales(totalSales)
                .beatRevenue(beatRevenue)
                .monthlyBeatRevenue(monthlyBeatRevenue)
                .averageRating(averageRating)
                .conversionRate(conversionRate)
                .topBeatId(topBeat.map(Beat::getId).orElse(null))
                .topBeatTitle(topBeat.map(Beat::getTitle).orElse(null))
                .beatBreakdown(beatBreakdown)
                .totalBookings(totalBookings)
                .upcomingBookings(upcomingBookings)
                .pendingBookingRequests(pendingBookingRequests)
                .bookingRevenue(bookingRevenue)
                .monthlyBookingRevenue(monthlyBookingRevenue)
                .totalRevenue(beatRevenue + bookingRevenue)
                .monthlyRevenue(monthlyBeatRevenue + monthlyBookingRevenue)
                .walletBalance(walletBalance)
                .build();
    }

    private BeatPerformanceResponse toBeatPerformance(
            Beat beat,
            Map<String, List<BeatPurchase>> purchasesByBeat,
            Map<String, List<BeatReview>> reviewsByBeat) {

        List<BeatPurchase> beatPurchases = purchasesByBeat.getOrDefault(beat.getId(), List.of());
        List<BeatReview> beatReviews = reviewsByBeat.getOrDefault(beat.getId(), List.of());

        long salesCount = beatPurchases.size();
        int revenue = beatPurchases.stream().mapToInt(BeatPurchase::getAmount).sum();

        Double averageRating = beatReviews.isEmpty()
                ? null
                : beatReviews.stream().mapToInt(BeatReview::getRating).average().orElse(0.0);

        return BeatPerformanceResponse.builder()
                .beatId(beat.getId())
                .title(beat.getTitle())
                .status(beat.getStatus().name())
                .coverUrl(beat.getCoverUrl())
                .thumbnailUrl(beat.getThumbnailUrl())
                .playCount(nullToZero(beat.getPlayCount()))
                .downloadCount(nullToZero(beat.getDownloadCount()))
                .likeCount(nullToZero(beat.getLikeCount()))
                .salesCount(salesCount)
                .revenue(revenue)
                .averageRating(averageRating)
                .build();
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
