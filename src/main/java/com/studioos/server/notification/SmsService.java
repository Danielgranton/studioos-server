package com.studioos.server.notification;

import com.africastalking.AfricasTalking;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class SmsService {

    private final AfricasTalkingProperties properties;

    public SmsService(AfricasTalkingProperties properties) {
        this.properties = properties;
    }

    @Async
    public CompletableFuture<Boolean> sendOtp(String phone, String otp) {
        return send(phone, "Your StudioOS code is: " + otp, "OTP");
    }

    @Async
    public CompletableFuture<Boolean> sendNotification(String phone, String message) {
        return send(phone, message, "notification");
    }

    private CompletableFuture<Boolean> send(String phone, String message, String kind) {
        try {
            com.africastalking.SmsService sms =
                    AfricasTalking.getService(AfricasTalking.SERVICE_SMS);
            var response = sms.send(message, new String[]{phone}, false);
            log.info("[SMS] Sent {} to {} | response: {}", kind, maskPhone(phone), response);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("[SMS] Failed to send {} to {}: {}", kind, maskPhone(phone), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return phone.substring(0, 4) + "****";
    }
}