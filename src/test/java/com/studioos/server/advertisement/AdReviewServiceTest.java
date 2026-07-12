package com.studioos.server.advertisement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import com.studioos.server.shared.enums.AdCreativeStatus;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdReviewServiceTest {

    @Mock
    private AdvertisementRepository advertisementRepository;
    @Mock
    private AdNotificationService adNotificationService;

    @InjectMocks
    private AdReviewService adReviewService;

    @Test
    void approveRejectsNonAdminUsers() {
        User user = User.builder().id(1).role(Role.USER).build();

        assertThatThrownBy(() -> adReviewService.approve(user, "ad-1"))
                .isInstanceOf(StudioosException.class)
                .hasMessageContaining("Only admins");
    }

    @Test
    void approveMovesPendingReviewAdToReady() {
        User admin = User.builder().id(1).role(Role.SUPER_ADMIN).build();
        Advertisement ad = Advertisement.builder()
                .id("ad-1")
                .status(AdCreativeStatus.PENDING_REVIEW)
                .build();

        when(advertisementRepository.findById("ad-1")).thenReturn(Optional.of(ad));
        when(advertisementRepository.save(any(Advertisement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adReviewService.approve(admin, "ad-1");

        org.assertj.core.api.Assertions.assertThat(ad.getStatus()).isEqualTo(AdCreativeStatus.READY);
        verify(adNotificationService).notifyAdApproved(ad);
    }
}
