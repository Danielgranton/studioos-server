package com.studioos.server.dashboard.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ProducerDashboardResponse {
    // Beat marketplace
    private long totalBeats;
    private long publishedBeats;
    private long draftBeats;
    private long pendingProcessing;
    private long totalPlays;
    private long totalDownloads;
    private long totalSales;
    private Integer beatRevenue;
    private Integer monthlyBeatRevenue;
    private Double averageRating;
    private Double conversionRate;
    private String topBeatId;
    private String topBeatTitle;
    private List<BeatPerformanceResponse> beatBreakdown;

    // Studio bookings
    private long totalBookings;
    private long upcomingBookings;
    private long pendingBookingRequests;
    private Integer bookingRevenue;
    private Integer monthlyBookingRevenue;

    // Combined
    private Integer totalRevenue;
    private Integer monthlyRevenue;
    private Integer walletBalance; 
}