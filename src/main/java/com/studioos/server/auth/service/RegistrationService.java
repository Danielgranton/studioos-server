package com.studioos.server.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.auth.communication.CommunicationClient;
import com.studioos.server.auth.communication.CommunicationRequestFactory;
import com.studioos.server.auth.dto.OtpSentResponse;
import com.studioos.server.auth.dto.RegisterRequest;
import com.studioos.server.auth.otp.OtpService;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final CommunicationClient communicationClient;
    private final CommunicationRequestFactory communicationRequestFactory;
    private final ProfileImageServiceClient profileImageServiceClient;
    private final PasswordService passwordService;

    @Transactional
    public OtpSentResponse register(RegisterRequest request) {
        validateRegistration(request);

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role(request.getRole())
                .passwordHash(passwordService.hash(request.getPassword()))
                .build();

        userRepository.save(user);

        if (request.getProfileImage() != null && !request.getProfileImage().isBlank()) {
            var image = profileImageServiceClient.processProfileImage(
                    request.getProfileImage(),
                    "users/" + user.getId() + "/profile");
            if (image != null) {
                user.setProfileImage(image.getOriginalUrl());
                user.setProfileImageLarge(image.getLargeUrl());
                user.setProfileImageMedium(image.getMediumUrl());
                user.setProfileImageThumbnail(image.getThumbnailUrl());
                userRepository.save(user);
            }
        }

        String otp = otpService.generateAndSave(request.getEmail());
        communicationClient.send(communicationRequestFactory.otp(request.getEmail(), request.getPhone(), otp));
        log.info("Registration OTP queued for: {} / {}", request.getEmail(), request.getPhone());

        return OtpSentResponse.builder()
                .message("Verification code sent to your email and phone")
                .maskedEmail(maskEmail(request.getEmail()))
                .maskedPhone(maskPhone(request.getPhone()))
                .build();
    }

    private void validateRegistration(RegisterRequest request) {
        if (request.getRole() == null || request.getRole() == Role.SUPER_ADMIN) {
            throw StudioosException.forbidden("Cannot self-register as a super admin");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw StudioosException.conflict("Email already in use");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw StudioosException.conflict("Phone number already in use");
        }
    }

    private String maskEmail(String email) {
        if (email == null) return null;
        String[] parts = email.split("@");
        return parts[0].substring(0, Math.min(3, parts[0].length())) + "***@" + parts[1];
    }

    private String maskPhone(String phone) {
        if (phone == null) return null;
        return phone.substring(0, Math.min(4, phone.length())) + "****";
    }
}
