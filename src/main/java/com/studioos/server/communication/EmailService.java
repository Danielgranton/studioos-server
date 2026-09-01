package com.studioos.server.communication;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${communication.email.from:}")
    private String fromEmail;

    @Value("${communication.email.from-name:StudioOS}")
    private String fromName;

    @Async
    public void sendOtp(String to, String otp) {
        send(to, "StudioOS - Your Verification Code",
                "Hi,\n\nYour StudioOS verification code is:\n\n  " + otp
                        + "\n\nThis code expires in 10 minutes.\n\n"
                        + "If you did not request this, please ignore this email.\n\n- StudioOS Team");
    }

    @Async
    public void sendNotification(String to, String subject, String body) {
        send(to, subject, body);
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            if (fromEmail != null && !fromEmail.isBlank()) {
                message.setFrom("\"" + fromName + "\" <" + fromEmail + ">");
            }
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
