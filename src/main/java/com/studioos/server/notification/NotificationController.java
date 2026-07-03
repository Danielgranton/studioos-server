package com.studioos.server.notification;

import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.shared.dto.PageResponse;
import com.studioos.server.notification.dto.NotificationResponse;
import com.studioos.server.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationServiceImpl notificationService;

    // ─── Get all my notifications (paginated, newest first) ───
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<NotificationResponse> response = notificationService.getMyNotifications(currentUser, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ─── Get unread notifications only ───
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getUnreadNotifications(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<NotificationResponse> response = notificationService.getUnreadNotifications(currentUser, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ─── Get unread count (for UI badge) ───
    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal User currentUser
    ) {
        long count = notificationService.getUnreadCount(currentUser);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    // ─── Mark single notification as read ───
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String notificationId
    ) {
        NotificationResponse response = notificationService.markAsRead(currentUser, notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", response));
    }

    // ─── Mark all as read ───
    @PatchMapping("/read/all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal User currentUser
    ) {
        notificationService.markAllAsRead(currentUser);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read"));
    }

    // ─── Delete notification ───
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @AuthenticationPrincipal User currentUser,
            @PathVariable String notificationId
    ) {
        notificationService.deleteNotification(currentUser, notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted"));
    }
}