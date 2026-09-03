package com.studioos.server.dashboard.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.studioos.server.shared.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class DashboardOverviewResponse {
    private DashboardUserSummary user;
    private ProfileCompletion profile;
    private DashboardMetrics metrics;
    private List<DashboardBooking> upcomingBookings;
    private List<DashboardActivity> recentActivity;
    private List<DashboardRecommendation> recommendations;
    private RoleWorkspace workspace;

    @Data
    @Builder
    @AllArgsConstructor
    public static class DashboardUserSummary {
        private Integer id;
        private String name;
        private String username;
        private Role role;
        private String profileImage;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class ProfileCompletion {
        private int percentage;
        private List<String> missingItems;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class DashboardMetrics {
        private long upcomingBookings;
        private long activeProjects;
        private long savedItems;
        private Long profileViews;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class DashboardBooking {
        private String id;
        private String title;
        private String counterpartyName;
        private String location;
        private LocalDateTime startsAt;
        private String status;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class DashboardActivity {
        private String id;
        private String type;
        private String title;
        private String description;
        private LocalDateTime occurredAt;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class DashboardRecommendation {
        private String id;
        private String type;
        private String title;
        private String subtitle;
        private String imageUrl;
        private String href;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class RoleWorkspace {
        private Role role;
        private long recommendationsCount;
        private long releaseCount;
        private long activeProjectCount;
        private long pendingRequestCount;
        private long serviceCount;
    }
}
