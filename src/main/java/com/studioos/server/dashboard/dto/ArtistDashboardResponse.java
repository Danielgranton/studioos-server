package com.studioos.server.dashboard.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ArtistDashboardResponse {
    // Bookings
    private long totalBookings;
    private long upcomingBookings;
    private long pendingBookings;
    private long pastBookings;
    private List<BookingSummaryResponse> bookingBreakdown;

    // Beat purchases
    private long totalPurchases;
    private Integer totalSpent;
    private List<PurchasedBeatResponse> purchaseBreakdown;

    // Liked beats
    private List<LikedBeatResponse> likedBeats;

    // Reviews given
    private List<ReviewGivenResponse> reviewsGiven;
}