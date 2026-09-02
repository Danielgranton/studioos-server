package com.studioos.server.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.studioos.server.auth.dto.AuthResponse;
import com.studioos.server.auth.dto.VerifyOtpRequest;
import com.studioos.server.auth.otp.OtpService;
import com.studioos.server.auth.service.SessionService;
import com.studioos.server.auth.service.TokenService;
import com.studioos.server.auth.service.UserLookupService;
import com.studioos.server.auth.service.VerificationService;
import com.studioos.server.user.User;
import com.studioos.server.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock private UserLookupService userLookupService;
    @Mock private OtpService otpService;
    @Mock private TokenService tokenService;
    @Mock private SessionService sessionService;
    @Mock private UserRepository userRepository;

    @InjectMocks private VerificationService verificationService;

    @Test
    void registrationVerificationMarksOnlyEmailAsVerified() {
        User user = User.builder().id(1).email("user@example.com").phone("+254700000000").build();
        VerifyOtpRequest request = request("user@example.com");
        AuthResponse auth = AuthResponse.builder().refreshToken("refresh").build();
        when(userLookupService.findByIdentifier(request.getIdentifier())).thenReturn(user);
        when(tokenService.issue(user)).thenReturn(auth);

        verificationService.verifyRegistration(request);

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.isPhoneVerified()).isFalse();
        assertThat(user.isAccountVerified()).isTrue();
        verify(userRepository).save(user);
        verify(otpService).verify(user.getEmail(), request.getCode());
    }

    @Test
    void loginVerificationDoesNotChangeVerificationFlags() {
        User user = User.builder().id(1).email("user@example.com").phone("+254700000000").build();
        VerifyOtpRequest request = request("+254700000000");
        AuthResponse auth = AuthResponse.builder().refreshToken("refresh").build();
        when(userLookupService.findByIdentifier(request.getIdentifier())).thenReturn(user);
        when(tokenService.issue(user)).thenReturn(auth);

        verificationService.verifyLogin(request);

        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.isPhoneVerified()).isFalse();
        assertThat(user.isAccountVerified()).isFalse();
        verify(userRepository, never()).save(user);
    }

    private VerifyOtpRequest request(String identifier) {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setIdentifier(identifier);
        request.setCode("123456");
        return request;
    }
}
