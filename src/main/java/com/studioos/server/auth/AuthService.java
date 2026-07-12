package com.studioos.server.auth;

import com.studioos.server.auth.dto.*;
import com.studioos.server.auth.otp.OtpService;
import com.studioos.server.notification.EmailService;
import com.studioos.server.notification.SmsService;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final SmsService smsService;

    // ─── STEP 1: Register — collect user info, send OTP ───
    @Transactional
    public OtpSentResponse register(RegisterRequest request) {
        if (request.getRole() == null || request.getRole() == com.studioos.server.shared.enums.Role.SUPER_ADMIN) {
            throw StudioosException.forbidden("Cannot self-register as a super admin");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw StudioosException.conflict("Email already in use");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw StudioosException.conflict("Phone number already in use");
        }

        // Save user (unverified — no verified flag needed, OTP is the gate)
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .role(request.getRole())
                .profileImage(request.getProfileImage())
                .build();

        userRepository.save(user);

        // Generate OTP and send to both email and phone
        String otp = otpService.generateAndSave(request.getEmail());
        emailService.sendOtp(request.getEmail(), otp);
        smsService.sendOtp(request.getPhone(), otp);

        log.info("Registration OTP sent to: {} / {}", request.getEmail(), request.getPhone());

        return OtpSentResponse.builder()
                .message("Verification code sent to your email and phone")
                .maskedEmail(maskEmail(request.getEmail()))
                .maskedPhone(maskPhone(request.getPhone()))
                .build();
    }

    // ─── STEP 2: Verify OTP after registration ───
    public AuthResponse verifyRegistration(VerifyOtpRequest request) {
        // Verify OTP against identifier (email or phone)
        otpService.verify(request.getIdentifier(), request.getCode());

        User user = findByIdentifier(request.getIdentifier());
        log.info("User verified and registered: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    // ─── STEP 1: Login — find user, send OTP ───
    public OtpSentResponse login(LoginRequest request) {
        User user = findByIdentifier(request.getIdentifier());

        // Send OTP to both channels
        String otp = otpService.generateAndSave(user.getEmail());
        emailService.sendOtp(user.getEmail(), otp);
        smsService.sendOtp(user.getPhone(), otp);

        log.info("Login OTP sent for user: {}", user.getEmail());

        return OtpSentResponse.builder()
                .message("Verification code sent to your email and phone")
                .maskedEmail(maskEmail(user.getEmail()))
                .maskedPhone(maskPhone(user.getPhone()))
                .build();
    }

    // ─── STEP 2: Verify OTP after login ───
    public AuthResponse verifyLogin(VerifyOtpRequest request) {
        otpService.verify(request.getIdentifier(), request.getCode());

        User user = findByIdentifier(request.getIdentifier());
        log.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    // ─── Resend OTP ───
    public OtpSentResponse resendOtp(LoginRequest request) {
        User user = findByIdentifier(request.getIdentifier());

        String otp = otpService.generateAndSave(user.getEmail());
        emailService.sendOtp(user.getEmail(), otp);
        smsService.sendOtp(user.getPhone(), otp);

        return OtpSentResponse.builder()
                .message("New verification code sent to your email and phone")
                .maskedEmail(maskEmail(user.getEmail()))
                .maskedPhone(maskPhone(user.getPhone()))
                .build();
    }

    // ─── Refresh token ───
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        final String email = jwtService.extractEmail(request.getRefreshToken());
        if (!jwtService.isTokenOfType(request.getRefreshToken(), JwtService.TOKEN_TYPE_REFRESH)) {
            throw StudioosException.unauthorized("Invalid refresh token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> StudioosException.notFound("User not found"));

        if (jwtService.isTokenExpired(request.getRefreshToken())) {
            throw StudioosException.unauthorized("Refresh token expired, please login again");
        }

        return buildAuthResponse(user);
    }

    // ─── Helpers ───
    private User findByIdentifier(String identifier) {
        return userRepository.findByEmailOrPhone(identifier, identifier)
                .orElseThrow(() -> StudioosException.notFound("No account found with this email or phone number"));
    }

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .build();
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
