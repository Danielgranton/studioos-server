package com.studioos.server.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.studioos.server.notification.dto.CreateNotificationRequest;
import com.studioos.server.shared.enums.NotificationType;
import com.studioos.server.user.User;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private com.studioos.server.user.UserRepository userRepository;

    @Mock
    private NotificationPreferenceService preferenceService;

    @Mock
    private NotificationOutboxService outboxService;

    @Mock
    private NotificationRateLimitService rateLimitService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void createNotificationQueuesEnabledExternalChannelsInTheSameFlow() {
        User user = User.builder()
                .id(7)
                .email("artist@example.com")
                .phone("+254700000000")
                .build();
        NotificationPreference preference = NotificationPreference.builder()
                .inAppEnabled(true)
                .emailEnabled(true)
                .smsEnabled(true)
                .build();
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setUserId(7);
        request.setType(NotificationType.NEW_MESSAGE);
        request.setTitle("New message");
        request.setMessage("You received a new message");

        when(userRepository.findById(7)).thenReturn(Optional.of(user));
        when(preferenceService.resolvePreference(user, NotificationType.NEW_MESSAGE)).thenReturn(preference);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rateLimitService.allowEmail(user)).thenReturn(true);
        when(rateLimitService.allowSms(user)).thenReturn(true);

        notificationService.createNotification(request);

        verify(notificationRepository).save(any(Notification.class));
        verify(outboxService).enqueueEmail(eq(7), any(), eq("artist@example.com"), eq("New message"), eq("You received a new message"));
        verify(outboxService).enqueueSms(eq(7), any(), eq("+254700000000"), eq("You received a new message"));
    }
}
