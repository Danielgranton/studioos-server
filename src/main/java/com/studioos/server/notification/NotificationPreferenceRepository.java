package com.studioos.server.notification;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.studioos.server.shared.enums.NotificationType;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, String> {
    List<NotificationPreference> findByUserId(Integer userId);
    Optional<NotificationPreference> findByUserIdAndNotificationType(Integer userId, NotificationType notificationType);
}
