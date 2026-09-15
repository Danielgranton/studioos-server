package com.studioos.server.notification;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.MeterRegistry;

import com.studioos.server.communication.EmailService;
import com.studioos.server.communication.SmsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxProcessor {

    private final NotificationOutboxService outboxService;
    private final EmailService emailService;
    private final SmsService smsService;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelayString = "${notification.outbox.poll-interval-ms:5000}")
    public void process() {
        for (NotificationOutbox event : outboxService.claimBatch(25)) {
            try {
                NotificationDeliveryResult result = switch (event.getChannel()) {
                    case EMAIL -> new NotificationDeliveryResult(
                            emailService.sendNotificationNow(event.getRecipient(), event.getSubject(), event.getBody()),
                            null
                    );
                    case SMS -> smsService.sendNotificationNowWithId(event.getRecipient(), event.getBody());
                };

                if (result.accepted()) {
                    outboxService.markSent(event.getId(), result.providerMessageId());
                    meterRegistry.counter("studioos.notifications.outbox.sent", "channel", event.getChannel().name()).increment();
                } else {
                    outboxService.markFailed(event.getId(), "Communication provider rejected the message");
                    meterRegistry.counter("studioos.notifications.outbox.failed", "channel", event.getChannel().name()).increment();
                }
            } catch (Exception exception) {
                log.error("Notification outbox delivery failed: {}", event.getId(), exception);
                outboxService.markFailed(event.getId(), exception.getMessage());
                meterRegistry.counter("studioos.notifications.outbox.failed", "channel", event.getChannel().name()).increment();
            }
        }
    }
}
