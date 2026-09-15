package com.studioos.server.communication;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import com.studioos.server.notification.NotificationDeliveryResult;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final SmsProvider smsProvider;

    @Async
    public CompletableFuture<Boolean> sendOtp(String phone, String otp) {
        return CompletableFuture.completedFuture(
                sendNow(phone, "Your StudioOS code is: " + otp, "OTP") != null
        );
    }

    @Async
    public CompletableFuture<Boolean> sendNotification(String phone, String message) {
        return CompletableFuture.completedFuture(sendNotificationNow(phone, message));
    }

    public boolean sendNotificationNow(String phone, String message) {
        return sendNotificationNowWithId(phone, message).accepted();
    }

    public NotificationDeliveryResult sendNotificationNowWithId(String phone, String message) {
        String messageId = sendNow(phone, message, "notification");
        return new NotificationDeliveryResult(messageId != null, messageId);
    }

    private String sendNow(String phone, String message, String kind) {
        try {
            String messageSid = smsProvider.send(phone, message);
            log.info("[SMS] Sent {} to {} | provider message: {}", kind, maskPhone(phone), messageSid);
            return messageSid;
        } catch (Exception e) {
            log.error("[SMS] Failed to send {} to {}: {}", kind, maskPhone(phone), e.getMessage(), e);
            return null;
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return phone.substring(0, 4) + "****";
    }
}
