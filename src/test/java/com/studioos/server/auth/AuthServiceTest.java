package com.studioos.server.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studioos.server.auth.dto.AuthResponse;
import com.studioos.server.auth.dto.LoginRequest;
import com.studioos.server.auth.dto.OtpSentResponse;
import com.studioos.server.auth.dto.RefreshTokenRequest;
import com.studioos.server.auth.dto.RegisterRequest;
import com.studioos.server.auth.dto.VerifyOtpRequest;
import com.studioos.server.auth.service.LoginService;
import com.studioos.server.auth.service.RefreshTokenService;
import com.studioos.server.auth.service.RegistrationService;
import com.studioos.server.auth.service.SessionService;
import com.studioos.server.auth.service.VerificationService;
import com.studioos.server.shared.enums.Role;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private RegistrationService registrationService;
    @Mock
    private LoginService loginService;
    @Mock
    private VerificationService verificationService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private SessionService sessionService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerDelegatesToRegistrationService() {
        RegisterRequest request = new RegisterRequest();
        request.setName("User");
        request.setEmail("user@example.com");
        request.setPhone("+254700000000");
        request.setRole(Role.USER);

        OtpSentResponse response = OtpSentResponse.builder().message("ok").build();
        when(registrationService.register(request)).thenReturn(response);

        assertThat(authService.register(request)).isSameAs(response);
        verify(registrationService).register(request);
    }

    @Test
    void loginDelegatesToLoginService() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("user@example.com");

        OtpSentResponse response = OtpSentResponse.builder().message("sent").build();
        when(loginService.login(request)).thenReturn(response);

        assertThat(authService.login(request)).isSameAs(response);
        verify(loginService).login(request);
    }

    @Test
    void verifyLoginDelegatesToVerificationService() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setIdentifier("user@example.com");
        request.setCode("123456");

        AuthResponse response = AuthResponse.builder().userId(1).build();
        when(verificationService.verifyLogin(request)).thenReturn(response);

        assertThat(authService.verifyLogin(request)).isSameAs(response);
        verify(verificationService).verifyLogin(request);
    }

    @Test
    void refreshTokenDelegatesToRefreshTokenService() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("token");

        AuthResponse response = AuthResponse.builder().userId(1).build();
        when(refreshTokenService.refresh(request)).thenReturn(response);

        assertThat(authService.refreshToken(request)).isSameAs(response);
        verify(refreshTokenService).refresh(request);
    }

    @Test
    void logoutDelegatesToSessionService() {
        com.studioos.server.auth.dto.LogoutRequest request = new com.studioos.server.auth.dto.LogoutRequest();
        request.setRefreshToken("token");

        authService.logout(request);

        verify(sessionService).logout(request);
    }
}
