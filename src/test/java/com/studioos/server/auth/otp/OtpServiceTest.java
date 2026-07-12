package com.studioos.server.auth.otp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.studioos.server.shared.exceptions.StudioosException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpRepository otpRepository;

    @Test
    void locksOtpAfterRepeatedInvalidAttempts() {
        AtomicReference<Otp> stored = new AtomicReference<>(Otp.builder()
                .identifier("user@example.com")
                .code("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .failedAttempts(0)
                .lockedUntil(null)
                .used(false)
                .build());

        when(otpRepository.findTopByIdentifierAndUsedFalseOrderByCreatedAtDesc("user@example.com"))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(otpRepository.save(any(Otp.class))).thenAnswer(invocation -> {
            Otp otp = invocation.getArgument(0);
            stored.set(otp);
            return otp;
        });

        OtpService otpService = new OtpService(otpRepository);

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> otpService.verify("user@example.com", "000000"))
                    .isInstanceOf(StudioosException.class)
                    .hasMessageContaining("Invalid OTP");
        }

        assertThat(stored.get().getFailedAttempts()).isEqualTo(5);
        assertThat(stored.get().getLockedUntil()).isNotNull();

        assertThatThrownBy(() -> otpService.verify("user@example.com", "123456"))
                .isInstanceOf(StudioosException.class)
                .hasMessageContaining("Too many invalid attempts");
    }
}
