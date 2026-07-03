package com.studioos.server.notification;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    Page<Notification> findByUserId(Integer userId, Pageable pageable);

    Page<Notification> findByUserIdAndIsRead(Integer userId, Boolean isRead, Pageable pageable);

    long countByUserIdAndIsReadFalse(Integer userId);

    List<Notification> findByUserIdOrderByCreatedAtDesc(Integer userId);
}