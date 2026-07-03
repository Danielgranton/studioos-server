package com.studioos.server.notification;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // ─── Send OTP (hardcoded template) ───
    @Async
    public void sendOtp(String to, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("StudioOS — Your Verification Code");
            message.setText(
                "Hi,\n\n" +
                "Your StudioOS verification code is:\n\n" +
                "  " + otp + "\n\n" +
                "This code expires in 10 minutes.\n\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "— StudioOS Team"
            );
            mailSender.send(message);
            log.info("OTP email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", to, e.getMessage());
        }
    }

    // ─── Send general notification ───
    @Async
    public void sendNotification(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Notification email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send notification email to {}: {}", to, e.getMessage());
        }
    }
}