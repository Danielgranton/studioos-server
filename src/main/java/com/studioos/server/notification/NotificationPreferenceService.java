package com.studioos.server.notification;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.notification.dto.NotificationPreferenceResponse;
import com.studioos.server.notification.dto.UpdateNotificationPreferenceRequest;
import com.studioos.server.shared.enums.NotificationType;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    @Transactional(readOnly = true)
    public List<NotificationPreferenceResponse> getPreferences(User currentUser) {
        return java.util.Arrays.stream(NotificationType.values())
                .map(type -> resolvePreference(currentUser, type))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<NotificationPreferenceResponse> updatePreferences(User currentUser, List<UpdateNotificationPreferenceRequest> updates) {
        if (updates == null || updates.isEmpty()) {
            throw StudioosException.badRequest("At least one preference update is required");
        }

        updates.forEach(update -> {
            NotificationPreference preference = preferenceRepository
                    .findByUserIdAndNotificationType(currentUser.getId(), update.getNotificationType())
                    .orElseGet(() -> defaultPreference(currentUser, update.getNotificationType()));

            if (update.getInAppEnabled() != null) {
                preference.setInAppEnabled(update.getInAppEnabled());
            }
            if (update.getEmailEnabled() != null) {
                preference.setEmailEnabled(update.getEmailEnabled());
            }
            if (update.getSmsEnabled() != null) {
                preference.setSmsEnabled(update.getSmsEnabled());
            }

            preferenceRepository.save(preference);
        });

        return getPreferences(currentUser);
    }

    @Transactional(readOnly = true)
    public NotificationPreference resolvePreference(User currentUser, NotificationType notificationType) {
        return preferenceRepository.findByUserIdAndNotificationType(currentUser.getId(), notificationType)
                .orElseGet(() -> defaultPreference(currentUser, notificationType));
    }

    @Transactional
    public List<NotificationPreferenceResponse> resetToRoleDefaults(User currentUser) {
        java.util.Arrays.stream(NotificationType.values()).forEach(type -> {
            NotificationPreference preference = preferenceRepository
                    .findByUserIdAndNotificationType(currentUser.getId(), type)
                    .orElseGet(() -> NotificationPreference.builder()
                            .userId(currentUser.getId())
                            .notificationType(type)
                            .build());

            applyRoleDefaults(preference, currentUser.getRole(), type);
            preferenceRepository.save(preference);
        });

        return getPreferences(currentUser);
    }

    private NotificationPreference defaultPreference(User currentUser, NotificationType notificationType) {
        return NotificationPreference.builder()
                .userId(currentUser.getId())
                .notificationType(notificationType)
                .inAppEnabled(true)
                .emailEnabled(defaultEmailEnabled(currentUser.getRole(), notificationType))
                .smsEnabled(defaultSmsEnabled(currentUser.getRole(), notificationType))
                .build();
    }

    private void applyRoleDefaults(NotificationPreference preference, Role role, NotificationType notificationType) {
        preference.setInAppEnabled(true);
        preference.setEmailEnabled(defaultEmailEnabled(role, notificationType));
        preference.setSmsEnabled(defaultSmsEnabled(role, notificationType));
    }

    private boolean defaultEmailEnabled(Role role, NotificationType notificationType) {
        if (role == Role.SUPER_ADMIN) {
            return isOperationalNotification(notificationType);
        }

        if (role == Role.PRODUCER) {
            return switch (notificationType) {
                case BOOKING_REQUEST,
                        BOOKING_CONFIRMED,
                        BOOKING_CANCELLED,
                        BOOKING_EXPIRED,
                        BEAT_SOLD,
                        WALLET_TRANSACTION,
                        ESCROW_ACTIVITY,
                        ADVERTISEMENT_REVIEW_REQUIRED,
                        ADVERTISEMENT_APPROVED,
                        ADVERTISEMENT_REJECTED,
                        AD_CAMPAIGN_PAYMENT_SUCCESS,
                        AD_CAMPAIGN_PAYMENT_FAILED,
                        AD_CAMPAIGN_LIVE -> true;
                default -> false;
            };
        }

        if (role == Role.ARTIST) {
            return switch (notificationType) {
                case BOOKING_REQUEST,
                        BOOKING_CONFIRMED,
                        BOOKING_CANCELLED,
                        BOOKING_EXPIRED,
                        PAYMENT_REQUEST,
                        PROJECT_UPDATE,
                        BEAT_PURCHASED,
                        BEAT_SHARED,
                        BEAT_PROCESSING_COMPLETED,
                        BEAT_PROCESSING_FAILED,
                        ADVERTISEMENT_APPROVED,
                        ADVERTISEMENT_REJECTED -> true;
                default -> false;
            };
        }

        return false;
    }

    private boolean defaultSmsEnabled(Role role, NotificationType notificationType) {
        if (role == Role.SUPER_ADMIN) {
            return false;
        }

        if (role == Role.PRODUCER) {
            return notificationType == NotificationType.BOOKING_REQUEST
                    || notificationType == NotificationType.BOOKING_CANCELLED
                    || notificationType == NotificationType.AD_CAMPAIGN_PAYMENT_FAILED;
        }

        if (role == Role.ARTIST) {
            return notificationType == NotificationType.BOOKING_CONFIRMED
                    || notificationType == NotificationType.BOOKING_CANCELLED
                    || notificationType == NotificationType.PAYMENT_REQUEST;
        }

        return false;
    }

    private boolean isOperationalNotification(NotificationType notificationType) {
        return switch (notificationType) {
            case BOOKING_REQUEST,
                    BOOKING_CONFIRMED,
                    BOOKING_CANCELLED,
                    BOOKING_EXPIRED,
                    BEAT_PROCESSING_COMPLETED,
                    BEAT_PROCESSING_FAILED,
                    ADVERTISEMENT_APPROVED,
                    ADVERTISEMENT_REJECTED,
                    ADVERTISEMENT_PROCESSING_COMPLETED,
                    ADVERTISEMENT_PROCESSING_FAILED,
                    ADVERTISEMENT_REVIEW_REQUIRED,
                    AD_CAMPAIGN_PAYMENT_SUCCESS,
                    AD_CAMPAIGN_PAYMENT_FAILED,
                    AD_CAMPAIGN_LIVE,
                    WALLET_TRANSACTION,
                    ESCROW_ACTIVITY -> true;
            default -> false;
        };
    }

    private NotificationPreferenceResponse toResponse(NotificationPreference preference) {
        return NotificationPreferenceResponse.builder()
                .notificationType(preference.getNotificationType())
                .inAppEnabled(preference.isInAppEnabled())
                .emailEnabled(preference.isEmailEnabled())
                .smsEnabled(preference.isSmsEnabled())
                .build();
    }
}
