package com.studioos.server.notification;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SmsService {

    /**
     * TODO: Integrate Africa's Talking SMS API
     * Dependency to add when ready:
     *   <dependency>
     *     <groupId>com.africastalking</groupId>
     *     <artifactId>client</artifactId>
     *     <version>3.4.4</version>
     *   </dependency>
     */
    @Async
    public void sendOtp(String phone, String otp) {
        // ─── MOCK: just log for now ───
        log.info("[SMS MOCK] Sending OTP {} to phone: {}", otp, maskPhone(phone));

        // ─── Replace with real implementation later:
        // AfricasTalking.initialize(username, apiKey);
        // SMSService sms = AfricasTalking.getService(AfricasTalking.SERVICE_SMS);
        // sms.send("Your StudioOS code is: " + otp, new String[]{phone}, "StudioOS");
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return phone.substring(0, 4) + "****";
    }
}