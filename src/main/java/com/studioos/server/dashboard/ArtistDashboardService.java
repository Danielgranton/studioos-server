package com.studioos.server.dashboard;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.beatmarketplace.Beat;
import com.studioos.server.beatmarketplace.BeatCollection;
import com.studioos.server.beatmarketplace.BeatCollectionItemRepository;
import com.studioos.server.beatmarketplace.BeatCollectionRepository;
import com.studioos.server.beatmarketplace.BeatDownload;
import com.studioos.server.beatmarketplace.BeatDownloadRepository;
import com.studioos.server.beatmarketplace.BeatLike;
import com.studioos.server.beatmarketplace.BeatLikeRepository;
import com.studioos.server.beatmarketplace.BeatPlayHistory;
import com.studioos.server.beatmarketplace.BeatPlayHistoryRepository;
import com.studioos.server.beatmarketplace.BeatPurchase;
import com.studioos.server.beatmarketplace.BeatPurchaseRepository;
import com.studioos.server.beatmarketplace.BeatRepository;
import com.studioos.server.beatmarketplace.BeatReview;
import com.studioos.server.beatmarketplace.BeatReviewRepository;
import com.studioos.server.booking.Booking;
import com.studioos.server.booking.BookingRepository;
import com.studioos.server.dashboard.dto.ArtistDashboardResponse;
import com.studioos.server.dashboard.dto.BookingSummaryResponse;
import com.studioos.server.dashboard.dto.CollectionSummaryResponse;
import com.studioos.server.dashboard.dto.DownloadHistoryResponse;
import com.studioos.server.dashboard.dto.LikedBeatResponse;
import com.studioos.server.dashboard.dto.PurchasedBeatResponse;
import com.studioos.server.dashboard.dto.RecentlyPlayedResponse;
import com.studioos.server.dashboard.dto.ReviewGivenResponse;
import com.studioos.server.shared.enums.BeatPaymentStatus;
import com.studioos.server.shared.enums.BookingStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArtistDashboardService {

    private final BookingRepository bookingRepository;
    private final BeatPurchaseRepository beatPurchaseRepository;
    private final BeatLikeRepository beatLikeRepository;
    private final BeatReviewRepository beatReviewRepository;
    private final BeatRepository beatRepository;
    private final BeatCollectionRepository beatCollectionRepository;
private final BeatCollectionItemRepository beatCollectionItemRepository;
private final BeatPlayHistoryRepository beatPlayHistoryRepository;
private final BeatDownloadRepository beatDownloadRepository;

    @Transactional(readOnly = true)
    public ArtistDashboardResponse getDashboard(Integer artistId) {

        LocalDateTime now = LocalDateTime.now();

        // ─── Bookings ───
        List<Booking> bookings = bookingRepository.findByArtistId(artistId);

        long totalBookings = bookings.size();

        long upcomingBookings = bookings.stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .filter(b -> b.getSessionDate() != null && b.getSessionDate().isAfter(now))
                .count();

        long pendingBookings = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING)
                .count();

        long pastBookings = bookings.stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .filter(b -> b.getSessionDate() != null && b.getSessionDate().isBefore(now))
                .count();

        List<BookingSummaryResponse> bookingBreakdown = bookings.stream()
                .map(this::toBookingSummary)
                .toList();

        // ─── Beat purchases ───
        List<BeatPurchase> purchases = beatPurchaseRepository.findByBuyerId(artistId);
        List<BeatPurchase> paidPurchases = purchases.stream()
                .filter(p -> p.getStatus() == BeatPaymentStatus.PAID)
                .toList();

        long totalPurchases = paidPurchases.size();
        int totalSpent = paidPurchases.stream().mapToInt(b -> b.getAmount()).sum();

        List<String> purchasedBeatIds = paidPurchases.stream().map(b -> b.getBeatId()).toList();
        Map<String, Beat> beatsById = fetchBeatsById(purchasedBeatIds);

        List<PurchasedBeatResponse> purchaseBreakdown = paidPurchases.stream()
                .map(p -> toPurchasedBeat(p, beatsById.get(p.getBeatId())))
                .toList();

        // ─── Liked beats ───
        List<BeatLike> likes = beatLikeRepository.findByUserId(artistId);
        List<String> likedBeatIds = likes.stream().map(b -> b.getBeatId()).toList();
        Map<String, Beat> likedBeatsById = fetchBeatsById(likedBeatIds);

        List<LikedBeatResponse> likedBeats = likes.stream()
                .map(l -> toLikedBeat(likedBeatsById.get(l.getBeatId())))
                .filter(r -> r != null)
                .toList();

        // ─── Reviews given ───
        List<BeatReview> reviews = beatReviewRepository.findByUserId(artistId);
        List<String> reviewedBeatIds = reviews.stream().map(b -> b.getBeatId()).toList();
        Map<String, Beat> reviewedBeatsById = fetchBeatsById(reviewedBeatIds);

        List<ReviewGivenResponse> reviewsGiven = reviews.stream()
                .map(r -> toReviewGiven(r, reviewedBeatsById.get(r.getBeatId())))
                .toList();

        
        // ─── Collections ───
        List<BeatCollection> collectionEntities = beatCollectionRepository.findByUserId(artistId);
        List<CollectionSummaryResponse> collections = collectionEntities.stream()
                .map(c -> CollectionSummaryResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .beatCount(beatCollectionItemRepository.findByIdCollectionId(c.getId()).size())
                        .createdAt(c.getCreatedAt())
                        .build())
                .toList();

        // ─── Recently played ───
        List<BeatPlayHistory> recentPlays = beatPlayHistoryRepository.findTop10ByUserIdOrderByPlayedAtDesc(artistId);
        List<String> recentPlayBeatIds = recentPlays.stream().map(BeatPlayHistory::getBeatId).toList();
        Map<String, Beat> recentPlayBeatsById = fetchBeatsById(recentPlayBeatIds);

        List<RecentlyPlayedResponse> recentlyPlayed = recentPlays.stream()
                .map(p -> {
                Beat beat = recentPlayBeatsById.get(p.getBeatId());
                return RecentlyPlayedResponse.builder()
                        .beatId(p.getBeatId())
                        .title(beat != null ? beat.getTitle() : null)
                        .coverUrl(beat != null ? beat.getCoverUrl() : null)
                        .playedAt(p.getPlayedAt())
                        .build();
                })
                .toList();

        // ─── Download history ───
        List<String> purchaseIds = paidPurchases.stream().map(BeatPurchase::getId).toList();
        List<BeatDownload> downloads = purchaseIds.isEmpty()
                ? List.of()
                : beatDownloadRepository.findByPurchaseIdIn(purchaseIds);

        Map<String, String> purchaseIdToBeatId = paidPurchases.stream()
                .collect(Collectors.toMap(BeatPurchase::getId, BeatPurchase::getBeatId));

        List<DownloadHistoryResponse> downloadHistory = downloads.stream()
                .map(d -> {
                String beatId = purchaseIdToBeatId.get(d.getPurchaseId());
                Beat beat = beatId != null ? beatsById.get(beatId) : null;
                return DownloadHistoryResponse.builder()
                        .beatId(beatId)
                        .title(beat != null ? beat.getTitle() : null)
                        .downloadedAt(d.getDownloadedAt())
                        .build();
                })
                .toList();
        

        return ArtistDashboardResponse.builder()
                .totalBookings(totalBookings)
                .upcomingBookings(upcomingBookings)
                .pendingBookings(pendingBookings)
                .pastBookings(pastBookings)
                .bookingBreakdown(bookingBreakdown)
                .collections(collections)
                .recentlyPlayed(recentlyPlayed)
                .downloadHistory(downloadHistory)
                .totalPurchases(totalPurchases)
                .totalSpent(totalSpent)
                .purchaseBreakdown(purchaseBreakdown)
                .likedBeats(likedBeats)
                .reviewsGiven(reviewsGiven)
                .build();
    }

    private Map<String, Beat> fetchBeatsById(List<String> beatIds) {
        if (beatIds.isEmpty()) return Map.of();
        return beatRepository.findAllById(beatIds).stream()
                .collect(Collectors.toMap(b -> b.getId(), b -> b));
    }

    private BookingSummaryResponse toBookingSummary(Booking booking) {
        return BookingSummaryResponse.builder()
                .bookingId(booking.getId())
                .studioName(booking.getStudio() != null ? booking.getStudio().getStudioName() : null)
                .sessionDate(booking.getSessionDate())
                .status(booking.getStatus().name())
                .totalPrice(booking.getTotalPrice())
                .build();
    }

    private PurchasedBeatResponse toPurchasedBeat(BeatPurchase purchase, Beat beat) {
        return PurchasedBeatResponse.builder()
                .beatId(purchase.getBeatId())
                .title(beat != null ? beat.getTitle() : null)
                .coverUrl(beat != null ? beat.getCoverUrl() : null)
                .licenseType(purchase.getLicenseId()) 
                .amount(purchase.getAmount())
                .purchasedAt(purchase.getPurchasedAt())
                .build();
    }

    private LikedBeatResponse toLikedBeat(Beat beat) {
        if (beat == null) return null;
        return LikedBeatResponse.builder()
                .beatId(beat.getId())
                .title(beat.getTitle())
                .coverUrl(beat.getCoverUrl())
                .producerId(beat.getProducerId())
                .build();
    }

    private ReviewGivenResponse toReviewGiven(BeatReview review, Beat beat) {
        return ReviewGivenResponse.builder()
                .beatId(review.getBeatId())
                .beatTitle(beat != null ? beat.getTitle() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}