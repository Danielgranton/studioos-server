package com.studioos.server.communication;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Primary
@Service
@RequiredArgsConstructor
public class DefaultCommunicationClient implements CommunicationClient {

    private final EmailService emailService;
    private final SmsService smsService;

    @Override
    public void send(CommunicationRequest request) {
        if (request.email() != null && !request.email().isBlank()) {
            if (request.type() == CommunicationType.OTP) {
                emailService.sendOtp(request.email(), request.metadata().get("code"));
            } else {
                emailService.sendNotification(request.email(), request.subject(), request.message());
            }
        }
        if (request.phone() != null && !request.phone().isBlank()) {
            smsService.sendNotification(request.phone(), request.message());
        }
    }
}
