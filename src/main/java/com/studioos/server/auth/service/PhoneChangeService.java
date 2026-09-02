package com.studioos.server.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.auth.dto.AuthResponse;
import com.studioos.server.auth.dto.OtpSentResponse;
import com.studioos.server.auth.dto.PhoneChangeRequest;
import com.studioos.server.auth.dto.VerifyOtpRequest;
import com.studioos.server.auth.otp.OtpService;
import com.studioos.server.communication.CommunicationClient;
import com.studioos.server.communication.CommunicationRequestFactory;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.shared.audit.AccountAuditService;
import com.studioos.server.shared.enums.AuditEventType;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PhoneChangeService {

    private final OtpService otpService;
    private final CommunicationClient communicationClient;
    private final CommunicationRequestFactory communicationRequestFactory;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final SessionService sessionService;
    private final AccountAuditService accountAuditService;

    @Transactional
    public OtpSentResponse request(User user, PhoneChangeRequest request) {
        requireUser(user);
        String newPhone = normalize(request == null ? null : request.getNewPhone());
        validatePhone(newPhone);

        if (newPhone.equals(user.getPhone())) {
            throw StudioosException.badRequest("This is already your account phone number");
        }
        if (userRepository.existsByPhone(newPhone)) {
            throw StudioosException.conflict("Phone number is already in use");
        }

        String code = otpService.generateAndSave(otpKey(user, newPhone));
        communicationClient.send(communicationRequestFactory.otp(null, newPhone, code));
        return OtpSentResponse.builder()
                .message("Phone change verification code sent")
                .maskedPhone(maskPhone(newPhone))
                .build();
    }

    @Transactional
    public AuthResponse verify(User user, VerifyOtpRequest request) {
        requireUser(user);
        String newPhone = normalize(request == null ? null : request.getIdentifier());
        validatePhone(newPhone);

        if (newPhone.equals(user.getPhone())) {
            throw StudioosException.badRequest("This is already your account phone number");
        }
        otpService.verify(otpKey(user, newPhone), request.getCode());

        if (userRepository.existsByPhone(newPhone)) {
            throw StudioosException.conflict("Phone number is already in use");
        }

        user.setPhone(newPhone);
        user.setPhoneVerified(true);
        userRepository.save(user);
        accountAuditService.record(AuditEventType.PHONE_CHANGED, user, "Account phone number changed");

        sessionService.logoutAllDevices(user);
        AuthResponse auth = tokenService.issue(user);
        sessionService.recordSession(user, auth.getRefreshToken());
        return auth;
    }

    private void requireUser(User user) {
        if (user == null) throw StudioosException.unauthorized("Authentication required");
    }

    private String otpKey(User user, String phone) {
        return "phone-change:" + user.getId() + ":" + phone;
    }

    private String normalize(String phone) {
        return phone == null ? "" : phone.replaceAll("[\\s()-]", "");
    }

    private void validatePhone(String phone) {
        if (!phone.matches("^\\+?[1-9]\\d{6,14}$")) {
            throw StudioosException.badRequest("Enter a valid phone number in international format");
        }
    }

    private String maskPhone(String phone) {
        return phone.length() < 4 ? "****" : phone.substring(0, Math.min(4, phone.length())) + "****";
    }
}
