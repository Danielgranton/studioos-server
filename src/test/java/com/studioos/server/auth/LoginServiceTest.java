package com.studioos.server.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.studioos.server.auth.dto.LoginRequest;
import com.studioos.server.auth.dto.OtpSentResponse;
import com.studioos.server.auth.otp.OtpService;
import com.studioos.server.auth.service.LoginService;
import com.studioos.server.auth.service.UserLookupService;
import com.studioos.server.communication.CommunicationClient;
import com.studioos.server.communication.CommunicationRequestFactory;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.User;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock private UserLookupService userLookupService;
    @Mock private OtpService otpService;
    @Mock private CommunicationClient communicationClient;
    @Mock private CommunicationRequestFactory communicationRequestFactory;

    @InjectMocks private LoginService loginService;

    @Test
    void unknownIdentifierReturnsGenericResponseWithoutSendingOtp() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("unknown@example.com");
        when(userLookupService.findByIdentifier(request.getIdentifier()))
                .thenThrow(StudioosException.notFound("No account found"));

        OtpSentResponse response = loginService.login(request);

        assertThat(response.getMessage()).contains("If an account exists");
        assertThat(response.getMaskedEmail()).isNull();
        assertThat(response.getMaskedPhone()).isNull();
        verify(otpService, never()).generateAndSave(request.getIdentifier());
        verify(communicationClient, never()).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void existingIdentifierGeneratesAndSendsOtp() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("user@example.com");
        User user = User.builder().email("user@example.com").phone("+254700000000").build();
        when(userLookupService.findByIdentifier(request.getIdentifier())).thenReturn(user);
        when(otpService.generateAndSave(user.getEmail())).thenReturn("123456");

        OtpSentResponse response = loginService.login(request);

        assertThat(response.getMessage()).contains("If an account exists");
        verify(otpService).generateAndSave(user.getEmail());
        verify(communicationClient).send(org.mockito.ArgumentMatchers.any());
    }
}
