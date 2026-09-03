package com.studioos.server.dashboard;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.beatmarketplace.BeatLikeRepository;
import com.studioos.server.booking.Booking;
import com.studioos.server.booking.BookingRepository;
import com.studioos.server.dashboard.dto.DashboardOverviewResponse;
import com.studioos.server.payment.AuditLogRepository;
import com.studioos.server.shared.enums.BookingStatus;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.studio.Studio;
import com.studioos.server.studio.StudioRepository;
import com.studioos.server.user.User;
import com.studioos.server.user.UserService;
import com.studioos.server.user.dto.UserProfileResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardOverviewService {

    private static final int MAX_BOOKINGS = 5;
    private static final int MAX_ACTIVITY = 5;

    private final UserService userService;
    private final BookingRepository bookingRepository;
    private final StudioRepository studioRepository;
    private final BeatLikeRepository beatLikeRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview(User currentUser) {
        UserProfileResponse profile = userService.getMyProfile(currentUser);
        List<Studio> studios = currentUser.getRole() == Role.PRODUCER
                ? studioRepository.findByOwnerId(currentUser.getId())
                : List.of();
        List<Booking> upcoming = upcomingBookings(currentUser, studios);

        return DashboardOverviewResponse.builder()
                .user(DashboardOverviewResponse.DashboardUserSummary.builder()
                        .id(profile.getId())
                        .name(profile.getName())
                        .username(profile.getUsername())
                        .role(profile.getRole())
                        .profileImage(profile.getProfileImageMedium())
                        .build())
                .profile(profileCompletion(profile))
                .metrics(DashboardOverviewResponse.DashboardMetrics.builder()
                        .upcomingBookings(upcoming.size())
                        .activeProjects(0)
                        .savedItems(beatLikeRepository.findByUserId(currentUser.getId()).size())
                        .profileViews(null)
                        .build())
                .upcomingBookings(upcoming.stream().map(booking -> toBooking(booking, currentUser)).toList())
                .recentActivity(auditLogRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId()).stream()
                        .limit(MAX_ACTIVITY)
                        .map(log -> DashboardOverviewResponse.DashboardActivity.builder()
                                .id(log.getId())
                                .type("SYSTEM")
                                .title(log.getEventType().name())
                                .description(log.getDescription())
                                .occurredAt(log.getCreatedAt())
                                .build())
                        .toList())
                .recommendations(List.of())
                .workspace(workspace(currentUser, studios))
                .build();
    }

    private List<Booking> upcomingBookings(User currentUser, List<Studio> studios) {
        List<Booking> bookings;
        if (currentUser.getRole() == Role.PRODUCER) {
            List<String> studioIds = studios.stream()
                    .map(Studio::getId)
                    .toList();
            bookings = studioIds.isEmpty() ? List.of() : bookingRepository.findByStudioIdIn(studioIds);
        } else if (currentUser.getRole() == Role.ARTIST) {
            bookings = bookingRepository.findByArtistId(currentUser.getId());
        } else {
            bookings = List.of();
        }

        return bookings.stream()
                .filter(booking -> booking.getStatus() != BookingStatus.CANCELLED)
                .filter(booking -> booking.getSessionDate() != null && booking.getSessionDate().isAfter(LocalDateTime.now()))
                .sorted(Comparator.comparing(Booking::getSessionDate))
                .limit(MAX_BOOKINGS)
                .toList();
    }

    private DashboardOverviewResponse.DashboardBooking toBooking(Booking booking, User currentUser) {
        boolean producer = currentUser.getRole() == Role.PRODUCER;
        String studioName = booking.getStudio() == null ? null : booking.getStudio().getStudioName();
        String location = booking.getStudio() == null ? null : booking.getStudio().getLocation();
        String counterparty = producer
                ? booking.getArtist() == null ? null : booking.getArtist().getName()
                : studioName;

        return DashboardOverviewResponse.DashboardBooking.builder()
                .id(booking.getId())
                .title(producer ? "Studio booking" : "Recording session")
                .counterpartyName(counterparty)
                .location(location)
                .startsAt(booking.getSessionDate())
                .status(booking.getStatus().name())
                .build();
    }

    private DashboardOverviewResponse.ProfileCompletion profileCompletion(UserProfileResponse profile) {
        List<String> missing = new java.util.ArrayList<>();
        int completed = 0;
        if (hasText(profile.getName())) completed++; else missing.add("Add your name");
        if (hasText(profile.getEmail())) completed++; else missing.add("Add your email");
        if (hasText(profile.getPhone())) completed++; else missing.add("Add your phone number");
        if (hasText(profile.getUsername())) completed++; else missing.add("Choose a username");
        if (hasText(profile.getBio())) completed++; else missing.add("Write a bio");
        if (hasText(profile.getLocation())) completed++; else missing.add("Add your location");
        if (hasText(profile.getGenre())) completed++; else missing.add("Add your genre");
        if (hasText(profile.getProfileImage())) completed++; else missing.add("Add a profile photo");

        return DashboardOverviewResponse.ProfileCompletion.builder()
                .percentage(completed * 100 / 8)
                .missingItems(missing)
                .build();
    }

    private DashboardOverviewResponse.RoleWorkspace workspace(User currentUser, List<Studio> studios) {
        Role role = currentUser.getRole();
        if (role == Role.PRODUCER) {
            List<String> studioIds = studios.stream().map(Studio::getId).toList();
            long pendingRequests = studioIds.isEmpty()
                    ? 0
                    : bookingRepository.findByStudioIdInAndStatus(studioIds, BookingStatus.PENDING).size();
            long serviceCount = studios.stream().mapToLong(studio -> studio.getServices().size()).sum();
            return DashboardOverviewResponse.RoleWorkspace.builder()
                    .role(role)
                    .pendingRequestCount(pendingRequests)
                    .serviceCount(serviceCount)
                    .build();
        }
        if (role == Role.ARTIST) {
            return DashboardOverviewResponse.RoleWorkspace.builder()
                    .role(role)
                    .activeProjectCount(0)
                    .releaseCount(0)
                    .build();
        }
        return DashboardOverviewResponse.RoleWorkspace.builder()
                .role(role)
                .recommendationsCount(0)
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
