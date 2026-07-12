package com.studioos.server.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import com.studioos.server.auth.dto.RefreshTokenRequest;
import com.studioos.server.auth.dto.RegisterRequest;
import com.studioos.server.auth.otp.OtpService;
import com.studioos.server.notification.EmailService;
import com.studioos.server.notification.SmsService;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private OtpService otpService;
    @Mock
    private JwtService jwtService;
    @Mock
    private EmailService emailService;
    @Mock
    private SmsService smsService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerRejectsSuperAdminSelfRegistration() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Admin");
        request.setEmail("admin@example.com");
        request.setPhone("+254700000000");
        request.setRole(Role.SUPER_ADMIN);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(StudioosException.class)
                .hasMessageContaining("super admin");
    }

    @Test
    void refreshTokenRejectsAccessToken() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("token");

        when(jwtService.extractEmail("token")).thenReturn("user@example.com");
        when(jwtService.isTokenOfType("token", JwtService.TOKEN_TYPE_REFRESH)).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(StudioosException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void refreshTokenAcceptsRefreshToken() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("token");

        User user = User.builder().id(1).email("user@example.com").name("User").role(Role.USER).refreshTokenVersion(0).build();
        when(jwtService.extractEmail("token")).thenReturn("user@example.com");
        when(jwtService.isTokenOfType("token", JwtService.TOKEN_TYPE_REFRESH)).thenReturn(true);
        when(jwtService.isTokenExpired("token")).thenReturn(false);
        when(jwtService.extractTokenVersion("token")).thenReturn(0);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.refreshToken(request);

        verify(userRepository).save(user);
    }

    @Test
    void refreshTokenRejectsRevokedTokenVersion() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("token");

        User user = User.builder().id(1).email("user@example.com").name("User").role(Role.USER).refreshTokenVersion(2).build();
        when(jwtService.extractEmail("token")).thenReturn("user@example.com");
        when(jwtService.isTokenOfType("token", JwtService.TOKEN_TYPE_REFRESH)).thenReturn(true);
        when(jwtService.isTokenExpired("token")).thenReturn(false);
        when(jwtService.extractTokenVersion("token")).thenReturn(1);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(StudioosException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    void verifyLoginUsesStoredEmailForOtpVerification() {
        com.studioos.server.auth.dto.VerifyOtpRequest request = new com.studioos.server.auth.dto.VerifyOtpRequest();
        request.setIdentifier("+254700000000");
        request.setCode("123456");

        User user = User.builder().id(1).email("user@example.com").phone("+254700000000").name("User").role(Role.USER).refreshTokenVersion(0).build();
        when(userRepository.findByEmailOrPhone("+254700000000", "+254700000000")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.verifyLogin(request);

        verify(otpService).verify("user@example.com", "123456");
    }
}
