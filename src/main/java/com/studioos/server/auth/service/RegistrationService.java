package com.studioos.server.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.communication.CommunicationClient;
import com.studioos.server.communication.CommunicationRequestFactory;
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

        User user = findOrCreatePendingUser(request);

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
        if (request.getRole() == Role.ADMIN
                || request.getRole() == Role.SUPER_ADMIN) {
            throw StudioosException.forbidden("Cannot self-register with a privileged role");
        }
    }

    private User findOrCreatePendingUser(RegisterRequest request) {
        User emailUser = userRepository.findByEmail(request.getEmail()).orElse(null);
        User phoneUser = userRepository.findByPhone(request.getPhone()).orElse(null);

        if (emailUser != null && emailUser.isAccountVerified()) {
            throw StudioosException.conflict("Email already in use");
        }
        if (phoneUser != null && phoneUser.isAccountVerified()) {
            throw StudioosException.conflict("Phone number already in use");
        }
        if (emailUser != null && phoneUser != null
                && !emailUser.getId().equals(phoneUser.getId())) {
            throw StudioosException.conflict("Email and phone number belong to different accounts");
        }

        User user = emailUser != null ? emailUser : phoneUser;
        if (user == null) {
            return User.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .phone(request.getPhone())
                    .role(request.getRole() == null ? Role.USER : request.getRole())
                    .passwordHash(passwordService.hash(request.getPassword()))
                    .build();
        }

        // Allow an unverified registration to be retried without creating duplicates.
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordService.hash(request.getPassword()));
        }
        return user;
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
