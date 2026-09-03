package com.studioos.server.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.studioos.server.beatmarketplace.BeatLikeRepository;
import com.studioos.server.booking.Booking;
import com.studioos.server.booking.BookingRepository;
import com.studioos.server.dashboard.dto.DashboardOverviewResponse;
import com.studioos.server.payment.AuditLogRepository;
import com.studioos.server.shared.enums.BookingPaymentStatus;
import com.studioos.server.shared.enums.BookingStatus;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.studio.StudioRepository;
import com.studioos.server.user.User;
import com.studioos.server.user.UserService;
import com.studioos.server.user.dto.UserProfileResponse;

@ExtendWith(MockitoExtension.class)
class DashboardOverviewServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private StudioRepository studioRepository;

    @Mock
    private BeatLikeRepository beatLikeRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private DashboardOverviewService service;

    @Test
    void buildsArtistOverviewFromRealDataAndExcludesCancelledBookings() {
        User user = User.builder().id(7).name("Artist").role(Role.ARTIST).build();
        UserProfileResponse profile = UserProfileResponse.builder()
                .id(7)
                .name("Artist")
                .email("artist@example.com")
                .role(Role.ARTIST)
                .build();
        LocalDateTime future = LocalDateTime.now().plusDays(2);
        Booking confirmed = Booking.builder()
                .id("booking-1")
                .artistId(7)
                .sessionDate(future)
                .status(BookingStatus.APPROVED)
                .paymentStatus(BookingPaymentStatus.PAID)
                .build();
        Booking cancelled = Booking.builder()
                .id("booking-2")
                .artistId(7)
                .sessionDate(future)
                .status(BookingStatus.CANCELLED)
                .paymentStatus(BookingPaymentStatus.BOOKED)
                .build();

        when(userService.getMyProfile(user)).thenReturn(profile);
        when(bookingRepository.findByArtistId(7)).thenReturn(List.of(confirmed, cancelled));
        when(beatLikeRepository.findByUserId(7)).thenReturn(List.of());
        when(auditLogRepository.findByUserIdOrderByCreatedAtDesc(7)).thenReturn(List.of());

        DashboardOverviewResponse response = service.getOverview(user);

        assertThat(response.getMetrics().getUpcomingBookings()).isEqualTo(1);
        assertThat(response.getUpcomingBookings()).extracting(DashboardOverviewResponse.DashboardBooking::getId)
                .containsExactly("booking-1");
        assertThat(response.getMetrics().getSavedItems()).isZero();
        assertThat(response.getProfile().getPercentage()).isEqualTo(25);
        assertThat(response.getWorkspace().getRole()).isEqualTo(Role.ARTIST);
    }
}
