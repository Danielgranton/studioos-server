package com.studioos.server.auth.otp;

import java.security.SecureRandom;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.studioos.server.shared.exceptions.StudioosException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 10;
    // Allow the initial OTP plus three explicit resend attempts.
    private static final int MAX_REQUESTS_PER_WINDOW = 4;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpStore otpStore;
    private final PasswordEncoder passwordEncoder;

    public String generateAndSave(String identifier) {
        int requestCount = otpStore.incrementRequestCount(identifier, OTP_EXPIRY_MINUTES * 60L);
        if (requestCount > MAX_REQUESTS_PER_WINDOW) {
            throw StudioosException.badRequest(
                    "Too many OTP requests. Please wait before requesting another code");
        }

        // Invalidate any existing OTPs for this identifier
        otpStore.invalidate(identifier);

        String code = generateCode();

        otpStore.save(identifier, passwordEncoder.encode(code), OTP_EXPIRY_MINUTES * 60L);
        log.info("OTP generated for identifier: {}", maskIdentifier(identifier));
        return code;
    }

    public void verify(String identifier, String code) {
        OtpStore.OtpRecord otp = otpStore.find(identifier);
        if (otp == null) {
            throw StudioosException.badRequest("No active OTP found. Please request a new one");
        }

        long now = Instant.now().getEpochSecond();
        if (otp.lockedUntilEpochSeconds() != null && now < otp.lockedUntilEpochSeconds()) {
            throw StudioosException.badRequest("Too many invalid attempts. Please request a new OTP");
        }

        if (!passwordEncoder.matches(code, otp.codeHash())) {
            int nextAttempts = otpStore.recordFailedAttempt(
                    identifier, otp.codeHash(), now, MAX_FAILED_ATTEMPTS, LOCKOUT_MINUTES * 60L);
            if (nextAttempts == -2) {
                throw StudioosException.badRequest("Too many invalid attempts. Please request a new OTP");
            }
            throw StudioosException.badRequest("Invalid OTP");
        }

        otpStore.consume(identifier, otp.codeHash());
        log.info("OTP verified for identifier: {}", maskIdentifier(identifier));
    }

    private String generateCode() {
        int code = RANDOM.nextInt((int) Math.pow(10, OTP_LENGTH));
        return String.format("%0" + OTP_LENGTH + "d", code);
    }

    // ─── Mask for logs (don't log full email/phone) ───
    private String maskIdentifier(String identifier) {
        if (identifier.contains("@")) {
            String[] parts = identifier.split("@");
            return parts[0].substring(0, Math.min(3, parts[0].length())) + "***@" + parts[1];
        }
        return identifier.substring(0, Math.min(4, identifier.length())) + "****";
    }
}
