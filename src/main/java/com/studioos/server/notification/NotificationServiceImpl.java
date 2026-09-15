package com.studioos.server.notification;

import com.studioos.server.shared.dto.PageResponse;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.notification.dto.CreateNotificationRequest;
import com.studioos.server.notification.dto.NotificationResponse;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationPreferenceService notificationPreferenceService;
    private final NotificationOutboxService notificationOutboxService;
    private final NotificationRateLimitService notificationRateLimitService;

    // ─── Create notification (respects per-type channel preferences) ───
    @Transactional
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> StudioosException.notFound("User not found"));

        NotificationPreference preference = notificationPreferenceService
                .resolvePreference(user, request.getType());

        Notification notification = null;
        if (preference.isInAppEnabled()) {
            notification = Notification.builder()
                    .userId(request.getUserId())
                    .type(request.getType())
                    .title(request.getTitle())
                    .message(request.getMessage())
                    .relatedEntityId(request.getRelatedEntityId())
                    .isRead(false)
                    .build();

            notificationRepository.save(notification);
            log.info("Notification saved to DB: {} for user: {}", notification.getId(), user.getEmail());
        } else {
            log.info("In-app notification suppressed for user {} type {}", user.getEmail(), request.getType());
        }

        if (preference.isEmailEnabled() && notificationRateLimitService.allowEmail(user)) {
            notificationOutboxService.enqueueEmail(
                    user.getId(),
                    notification == null ? null : notification.getId(),
                    user.getEmail(),
                    request.getTitle(),
                    request.getMessage()
            );
        }

        if (preference.isSmsEnabled()
                && user.getPhone() != null
                && !user.getPhone().isEmpty()
                && notificationRateLimitService.allowSms(user)) {
            notificationOutboxService.enqueueSms(
                    user.getId(),
                    notification == null ? null : notification.getId(),
                    user.getPhone(),
                    request.getMessage()
            );
        }

        return notification == null ? null : toResponse(notification);
    }

    // ─── Get user's notifications (paginated) ───
    public PageResponse<NotificationResponse> getMyNotifications(User currentUser, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.from(
                notificationRepository.findByUserId(currentUser.getId(), pageable)
                        .map(this::toResponse)
        );
    }

    // ─── Get unread notifications only ───
    public PageResponse<NotificationResponse> getUnreadNotifications(User currentUser, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PageResponse.from(
                notificationRepository.findByUserIdAndIsRead(currentUser.getId(), false, pageable)
                        .map(this::toResponse)
        );
    }

    // ─── Get unread count (for badge) ───
    public long getUnreadCount(User currentUser) {
        return notificationRepository.countByUserIdAndIsReadFalse(currentUser.getId());
    }

    // ─── Mark single notification as read ───
    @Transactional
    public NotificationResponse markAsRead(User currentUser, String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> StudioosException.notFound("Notification not found"));

        if (!notification.getUserId().equals(currentUser.getId())) {
            throw StudioosException.forbidden("This notification does not belong to you");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
        log.info("Notification marked as read: {}", notificationId);
        return toResponse(notification);
    }

    // ─── Mark all notifications as read ───
    @Transactional
    public void markAllAsRead(User currentUser) {
        int updated = notificationRepository.markAllAsReadByUserId(currentUser.getId());
        log.info("Marked {} notifications as read for user: {}", updated, currentUser.getId());
    }

    // ─── Delete notification ───
    @Transactional
    public void deleteNotification(User currentUser, String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> StudioosException.notFound("Notification not found"));

        if (!notification.getUserId().equals(currentUser.getId())) {
            throw StudioosException.forbidden("This notification does not belong to you");
        }

        notificationRepository.delete(notification);
        log.info("Notification deleted: {}", notificationId);
    }

    // ─── Helper ───
    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .relatedEntityId(notification.getRelatedEntityId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
