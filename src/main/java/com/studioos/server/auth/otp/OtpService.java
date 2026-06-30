package com.studioos.server.auth.otp;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.studioos.server.shared.exceptions.StudioosException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpRepository otpRepository;

    public String generateAndSave(String identifier) {
        // Invalidate any existing OTPs for this identifier
        otpRepository.invalidateAllForIdentifier(identifier);

        String code = generateCode();

        Otp otp = Otp.builder()
                .identifier(identifier)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .build();

        otpRepository.save(otp);
        log.info("OTP generated for identifier: {}", maskIdentifier(identifier));
        return code;
    }

    public void verify(String identifier, String code) {
        Otp otp = otpRepository
                .findTopByIdentifierAndUsedFalseOrderByCreatedAtDesc(identifier)
                .orElseThrow(() -> StudioosException.badRequest("No active OTP found. Please request a new one"));

        if (otp.isExpired()) {
            throw StudioosException.badRequest("OTP has expired. Please request a new one");
        }

        if (!otp.getCode().equals(code)) {
            throw StudioosException.badRequest("Invalid OTP");
        }

        // Mark as used
        otp.setUsed(true);
        otpRepository.save(otp);
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