package com.studioos.server.notification;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRetentionService {

    private final NotificationRepository notificationRepository;

    @Value("${notification.retention.days:90}")
    private long configuredRetentionDays;

    @Scheduled(cron = "${notification.retention.cron:0 0 3 * * *}")
    @Transactional
    public void deleteExpiredReadNotifications() {
        LocalDateTime cutoff = LocalDateTime.now()
                .minusDays(Math.max(1, retentionDays()));
        int deleted = notificationRepository.deleteReadBefore(cutoff);
        if (deleted > 0) {
            log.info("Deleted {} read notifications older than {}", deleted, cutoff);
        }
    }

    private long retentionDays() {
        return configuredRetentionDays;
    }
}
