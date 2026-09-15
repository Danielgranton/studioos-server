package com.studioos.server.notification;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    Page<Notification> findByUserId(Integer userId, Pageable pageable);

    Page<Notification> findByUserIdAndIsRead(Integer userId, Boolean isRead, Pageable pageable);

    long countByUserIdAndIsReadFalse(Integer userId);

    List<Notification> findByUserIdOrderByCreatedAtDesc(Integer userId);

    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.isRead = true
            WHERE n.userId = :userId
              AND (n.isRead = false OR n.isRead IS NULL)
            """)
    int markAllAsReadByUserId(@Param("userId") Integer userId);

    @Modifying
    @Query("""
            DELETE FROM Notification n
            WHERE n.createdAt < :cutoff
              AND n.isRead = true
            """)
    int deleteReadBefore(@Param("cutoff") LocalDateTime cutoff);
}
