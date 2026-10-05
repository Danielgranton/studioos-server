package com.studioos.server.beatmarketplace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.studioos.server.beatmarketplace.dto.PurchaseBeatRequest;
import com.studioos.server.notification.NotificationServiceImpl;
import com.studioos.server.notification.dto.CreateNotificationRequest;
import com.studioos.server.payment.PaymentService;
import com.studioos.server.shared.enums.BeatPaymentStatus;
import com.studioos.server.shared.enums.BeatStatus;
import com.studioos.server.shared.enums.LicenseType;
import com.studioos.server.shared.enums.NotificationType;
import com.studioos.server.shared.enums.TransactionType;
import com.studioos.server.shared.events.TransactionResolvedEvent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BeatPurchaseServiceTest {

    @Mock private BeatRepository beatRepository;
    @Mock private BeatLicenseRepository beatLicenseRepository;
    @Mock private BeatPurchaseRepository beatPurchaseRepository;
    @Mock private PaymentService paymentService;
    @Mock private NotificationServiceImpl notificationService;

    @InjectMocks private BeatPurchaseService beatPurchaseService;

    @Test
    void retryBySameBuyerReusesPendingExclusivePurchaseWithoutSendingAnotherPrompt() {
        Beat beat = readyBeat();
        BeatLicense license = exclusiveLicense();
        BeatPurchase pending = BeatPurchase.builder()
                .id("purchase-1")
                .beatId(beat.getId())
                .buyerId(10)
                .licenseId(license.getId())
                .transactionId("transaction-1")
                .status(BeatPaymentStatus.PENDING)
                .isExclusive(true)
                .build();
        when(beatRepository.findById(beat.getId())).thenReturn(Optional.of(beat));
        when(beatLicenseRepository.findById(license.getId())).thenReturn(Optional.of(license));
        when(beatLicenseRepository.findByIdForUpdate(license.getId())).thenReturn(Optional.of(license));
        when(beatPurchaseRepository.findFirstByLicenseIdAndBuyerIdAndStatusOrderByPurchasedAtDesc(
                license.getId(), 10, BeatPaymentStatus.PENDING)).thenReturn(Optional.of(pending));

        var result = beatPurchaseService.initiatePurchase(10, beat.getId(), request(license.getId()));

        assertThat(result.getPurchaseId()).isEqualTo("purchase-1");
        assertThat(result.getTransactionId()).isEqualTo("transaction-1");
        assertThat(result.isReusedExistingRequest()).isTrue();
        verify(paymentService, never()).initiateBeatPurchasePayment(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void anotherBuyerCannotStartStkForReservedExclusiveLicense() {
        Beat beat = readyBeat();
        BeatLicense license = exclusiveLicense();
        BeatPurchase pending = BeatPurchase.builder()
                .id("purchase-1")
                .beatId(beat.getId())
                .buyerId(11)
                .licenseId(license.getId())
                .status(BeatPaymentStatus.PENDING)
                .isExclusive(true)
                .build();
        when(beatRepository.findById(beat.getId())).thenReturn(Optional.of(beat));
        when(beatLicenseRepository.findById(license.getId())).thenReturn(Optional.of(license));
        when(beatLicenseRepository.findByIdForUpdate(license.getId())).thenReturn(Optional.of(license));
        when(beatPurchaseRepository.findFirstByLicenseIdAndBuyerIdAndStatusOrderByPurchasedAtDesc(
                license.getId(), 10, BeatPaymentStatus.PENDING)).thenReturn(Optional.empty());
        when(beatPurchaseRepository.existsByBeatIdAndBuyerIdAndStatus(
                beat.getId(), 10, BeatPaymentStatus.PAID)).thenReturn(false);
        when(beatPurchaseRepository.findByLicenseIdAndStatusIn(
                license.getId(), List.of(BeatPaymentStatus.PENDING, BeatPaymentStatus.PAID)))
                .thenReturn(List.of(pending));

        assertThatThrownBy(() -> beatPurchaseService.initiatePurchase(10, beat.getId(), request(license.getId())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already being purchased");

        verify(paymentService, never()).initiateBeatPurchasePayment(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void producerIsNotifiedWhenDarajaConfirmsPurchaseFailure() {
        Beat beat = readyBeat();
        BeatPurchase purchase = BeatPurchase.builder()
                .id("purchase-failed")
                .beatId(beat.getId())
                .buyerId(10)
                .licenseId("license-1")
                .transactionId("transaction-failed")
                .status(BeatPaymentStatus.PENDING)
                .isExclusive(true)
                .build();
        when(beatPurchaseRepository.findByTransactionId("transaction-failed"))
                .thenReturn(Optional.of(purchase));
        when(beatRepository.findById(beat.getId())).thenReturn(Optional.of(beat));

        beatPurchaseService.onTransactionResolved(
                new TransactionResolvedEvent("transaction-failed", TransactionType.BEAT_PURCHASE, false));

        assertThat(purchase.getStatus()).isEqualTo(BeatPaymentStatus.FAILED);
        var captor = org.mockito.ArgumentCaptor.forClass(CreateNotificationRequest.class);
        verify(notificationService).createNotification(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(beat.getProducerId());
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.BEAT_PURCHASE_FAILED);
    }

    private static Beat readyBeat() {
        return Beat.builder()
                .id("beat-1")
                .producerId(22)
                .studioId("studio-1")
                .title("Test beat")
                .status(BeatStatus.READY)
                .build();
    }

    private static BeatLicense exclusiveLicense() {
        return BeatLicense.builder()
                .id("license-1")
                .beatId("beat-1")
                .type(LicenseType.EXCLUSIVE)
                .price(1000)
                .exclusive(true)
                .active(true)
                .build();
    }

    private static PurchaseBeatRequest request(String licenseId) {
        PurchaseBeatRequest request = new PurchaseBeatRequest();
        request.setLicenseId(licenseId);
        request.setPhoneNumber("+254700000000");
        return request;
    }
}
