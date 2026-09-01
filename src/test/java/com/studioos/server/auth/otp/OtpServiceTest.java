package com.studioos.server.auth.otp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import com.studioos.server.shared.exceptions.StudioosException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpStore otpStore;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void locksOtpAfterRepeatedInvalidAttempts() {
        AtomicReference<OtpStore.OtpRecord> stored = new AtomicReference<>(
                new OtpStore.OtpRecord("hashed-valid", 0, null));

        when(otpStore.find("user@example.com")).thenAnswer(invocation -> stored.get());
        when(passwordEncoder.matches("000000", "hashed-valid")).thenReturn(false);
        when(otpStore.recordFailedAttempt(any(), any(), anyLong(), anyInt(), anyLong()))
                .thenAnswer(invocation -> {
                    int attempts = stored.get().failedAttempts() + 1;
                    stored.set(new OtpStore.OtpRecord(
                            "hashed-valid", attempts, attempts >= 5 ? Long.MAX_VALUE : null));
                    return attempts;
        });

        OtpService otpService = new OtpService(otpStore, passwordEncoder);

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> otpService.verify("user@example.com", "000000"))
                    .isInstanceOf(StudioosException.class)
                    .hasMessageContaining("Invalid OTP");
        }

        assertThat(stored.get().failedAttempts()).isEqualTo(5);
        assertThat(stored.get().lockedUntilEpochSeconds()).isNotNull();

        assertThatThrownBy(() -> otpService.verify("user@example.com", "123456"))
                .isInstanceOf(StudioosException.class)
                .hasMessageContaining("Too many invalid attempts");
    }
}
