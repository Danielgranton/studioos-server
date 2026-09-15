package com.studioos.server.notification;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationOutboxService {

    private static final int MAX_ATTEMPTS = 8;
    private static final int MAX_ERROR_LENGTH = 4000;

    private final NotificationOutboxRepository outboxRepository;

    public void enqueueEmail(Integer userId, String notificationId, String recipient, String subject, String body) {
        enqueue(userId, notificationId, NotificationChannel.EMAIL, recipient, subject, body);
    }

    public void enqueueSms(Integer userId, String notificationId, String recipient, String body) {
        enqueue(userId, notificationId, NotificationChannel.SMS, recipient, null, body);
    }

    private void enqueue(
            Integer userId,
            String notificationId,
            NotificationChannel channel,
            String recipient,
            String subject,
            String body
    ) {
        outboxRepository.save(NotificationOutbox.builder()
                .userId(userId)
                .notificationId(notificationId)
                .channel(channel)
                .recipient(recipient)
                .subject(subject)
                .body(body)
                .status(NotificationOutboxStatus.PENDING)
                .attempts(0)
                .nextAttemptAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public List<NotificationOutbox> claimBatch(int batchSize) {
        LocalDateTime now = LocalDateTime.now();
        List<NotificationOutbox> events = outboxRepository.claimReady(
                List.of(
                        NotificationOutboxStatus.PENDING.name(),
                        NotificationOutboxStatus.RETRY.name(),
                        NotificationOutboxStatus.PROCESSING.name()
                ),
                now,
                batchSize
        );
        events.forEach(event -> {
            event.setStatus(NotificationOutboxStatus.PROCESSING);
            event.setLastAttemptAt(now);
            // The timestamp doubles as a lease so crashed workers can be recovered.
            event.setNextAttemptAt(now.plusMinutes(10));
        });
        return events;
    }

    @Transactional
    public void markSent(String id, String providerMessageId) {
        outboxRepository.findById(id).ifPresent(event -> {
            event.setStatus(NotificationOutboxStatus.SENT);
            event.setSentAt(LocalDateTime.now());
            event.setProviderMessageId(providerMessageId);
            event.setLastError(null);
        });
    }

    @Transactional
    public void markFailed(String id, String error) {
        outboxRepository.findById(id).ifPresent(event -> {
            int attempts = event.getAttempts() + 1;
            event.setAttempts(attempts);
            event.setLastError(error == null ? "Unknown delivery error" : error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH)));

            if (attempts >= MAX_ATTEMPTS) {
                event.setStatus(NotificationOutboxStatus.FAILED);
                event.setFailedAt(LocalDateTime.now());
                return;
            }

            event.setStatus(NotificationOutboxStatus.RETRY);
            event.setNextAttemptAt(LocalDateTime.now().plusMinutes(Math.min(60, 1L << Math.min(attempts, 6))));
        });
    }
}
