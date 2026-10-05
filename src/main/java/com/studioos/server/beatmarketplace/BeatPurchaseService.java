package com.studioos.server.beatmarketplace;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.beatmarketplace.dto.BeatPurchaseInitiationResponse;
import com.studioos.server.beatmarketplace.dto.PurchaseBeatRequest;
import com.studioos.server.notification.NotificationServiceImpl;
import com.studioos.server.notification.dto.CreateNotificationRequest;
import com.studioos.server.payment.PaymentService;
import com.studioos.server.payment.Transaction;
import com.studioos.server.shared.enums.BeatPaymentStatus;
import com.studioos.server.shared.enums.BeatStatus;
import com.studioos.server.shared.enums.BeatVisibility;
import com.studioos.server.shared.enums.LicenseType;
import com.studioos.server.shared.enums.NotificationType;
import com.studioos.server.shared.enums.TransactionType;
import com.studioos.server.shared.events.TransactionResolvedEvent;

import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BeatPurchaseService {

    private final BeatRepository beatRepository;
    private final BeatLicenseRepository beatLicenseRepository;
    private final BeatPurchaseRepository beatPurchaseRepository;
    private final PaymentService paymentService;
    private final NotificationServiceImpl notificationService;

    @Transactional
    public BeatPurchaseInitiationResponse initiatePurchase(Integer buyerId, String beatId, PurchaseBeatRequest request) {
        Beat beat = beatRepository.findById(beatId)
                .orElseThrow(() -> new IllegalArgumentException("Beat not found: " + beatId));
        if (beat.getStatus() != BeatStatus.READY) {
            throw new IllegalStateException("Beat is not available for purchase: " + beat.getStatus());
        }

        BeatLicense license = beatLicenseRepository.findById(request.getLicenseId())
                .orElseThrow(() -> new IllegalArgumentException("License not found: " + request.getLicenseId()));
        // Serialize exclusive check-and-reserve across app instances before any STK side effect.
        if (Boolean.TRUE.equals(license.getExclusive())) {
            license = beatLicenseRepository.findByIdForUpdate(request.getLicenseId())
                    .orElseThrow(() -> new IllegalArgumentException("License not found: " + request.getLicenseId()));
        }
        if (!license.getBeatId().equals(beatId)) {
            throw new IllegalArgumentException("License " + license.getId() + " does not belong to beat " + beatId);
        }
        if (!Boolean.TRUE.equals(license.getActive())) {
            throw new IllegalStateException("This license is no longer available for purchase");
        }

        boolean isExclusive = Boolean.TRUE.equals(license.getExclusive());
        if (isExclusive) {
            BeatPurchase existingPending = beatPurchaseRepository
                    .findFirstByLicenseIdAndBuyerIdAndStatusOrderByPurchasedAtDesc(
                            license.getId(), buyerId, BeatPaymentStatus.PENDING)
                    .orElse(null);
            if (existingPending != null) {
                log.info("Reusing pending exclusive beat purchase {} for buyer {}", existingPending.getId(), buyerId);
                return purchaseResponse(existingPending, true);
            }
        }

        boolean alreadyOwned = beatPurchaseRepository.existsByBeatIdAndBuyerIdAndStatus(
                beatId, buyerId, BeatPaymentStatus.PAID);
        if (alreadyOwned) {
            throw new IllegalStateException("You have already purchased a license for this beat");
        }

        if (isExclusive && !beatPurchaseRepository.findByLicenseIdAndStatusIn(
                license.getId(), List.of(BeatPaymentStatus.PENDING, BeatPaymentStatus.PAID)).isEmpty()) {
            throw new IllegalStateException("This exclusive license is already being purchased or has been sold");
        }

        BeatPurchase purchase = BeatPurchase.builder()
                .beatId(beatId)
                .buyerId(buyerId)
                .licenseId(license.getId())
                .amount(license.getPrice())
                .status(BeatPaymentStatus.PENDING)
                .isExclusive(isExclusive)
                .build();
        try {
            // Force the unique reservation constraint to be checked before calling Safaricom.
            purchase = beatPurchaseRepository.saveAndFlush(purchase);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                    "This exclusive license is already being purchased or has been sold. No payment prompt was sent.");
        }

        Transaction transaction = paymentService.initiateBeatPurchasePayment(
                buyerId,
                beat.getStudioId(),
                license.getPrice(),
                request.getPhoneNumber(),
                "Beat purchase: " + beat.getTitle() + " (" + license.getType() + " license)");

        purchase.setTransactionId(transaction.getId());
        beatPurchaseRepository.save(purchase);
        return purchaseResponse(purchase, false);
    }

    private BeatPurchaseInitiationResponse purchaseResponse(BeatPurchase purchase, boolean reused) {
        return BeatPurchaseInitiationResponse.builder()
                .purchaseId(purchase.getId())
                .transactionId(purchase.getTransactionId())
                .status(purchase.getStatus().name())
                .reusedExistingRequest(reused)
                .build();
    }

    @EventListener
    @Transactional
    public void onTransactionResolved(TransactionResolvedEvent event) {

        if (event.getType() != TransactionType.BEAT_PURCHASE) {
            return; // not ours — booking payments and any future transaction types are ignored here
        }

        BeatPurchase purchase = beatPurchaseRepository.findByTransactionId(event.getTransactionId())
                .orElse(null);

        if (purchase == null) {
            log.warn("No BeatPurchase found for resolved transaction {}", event.getTransactionId());
            return;
        }

        if (purchase.getStatus() != BeatPaymentStatus.PENDING) {
            log.warn("Ignoring duplicate transaction-resolved event for purchase {} (already {})",
                    purchase.getId(), purchase.getStatus());
            return;
        }

        if (!event.isSuccess()) {
            purchase.setStatus(BeatPaymentStatus.FAILED);
            beatPurchaseRepository.save(purchase);
            notifyPurchaseFailure(purchase);
            return;
        }

        purchase.setStatus(BeatPaymentStatus.PAID);
        beatPurchaseRepository.save(purchase);

        BeatLicense license = beatLicenseRepository.findById(purchase.getLicenseId())
                .orElseThrow(() -> new IllegalStateException("License not found: " + purchase.getLicenseId()));

        Beat beat = beatRepository.findById(purchase.getBeatId())
                .orElseThrow(() -> new IllegalStateException("Beat not found: " + purchase.getBeatId()));

        if (license.getType() == LicenseType.EXCLUSIVE) {
            license.setActive(false);
            beatLicenseRepository.save(license);

            beat.setExclusiveSold(true);
            beat.setVisibility(BeatVisibility.UNLISTED);
            beatRepository.save(beat);
        }

        notifyPurchaseSuccess(purchase, beat, license);
    }

    private void notifyPurchaseSuccess(BeatPurchase purchase, Beat beat, BeatLicense license) {
        // Notification failures must not roll back an already-successful purchase —
        try {
            notificationService.createNotification(
                    buildRequest(purchase.getBuyerId(), NotificationType.BEAT_PURCHASED,
                            "Purchase successful",
                            "You've purchased the " + license.getType() + " license for \"" + beat.getTitle() + "\".",
                            purchase.getId())
            );

            notificationService.createNotification(
                    buildRequest(beat.getProducerId(), NotificationType.BEAT_SOLD,
                            "You sold a beat!",
                            "\"" + beat.getTitle() + "\" just sold under a " + license.getType() + " license.",
                            purchase.getId())
            );
        } catch (Exception e) {
            log.error("Failed to send purchase notifications for purchase {}: {}", purchase.getId(), e.getMessage());
        }
    }

    private void notifyPurchaseFailure(BeatPurchase purchase) {
        Beat beat = beatRepository.findById(purchase.getBeatId()).orElse(null);
        if (beat == null) return;
        try {
            notificationService.createNotification(
                    buildRequest(beat.getProducerId(), NotificationType.BEAT_PURCHASE_FAILED,
                            "Beat purchase did not complete",
                            "A buyer's payment for \"" + beat.getTitle()
                                    + "\" was confirmed as failed. The license is available again.",
                            purchase.getId()));
        } catch (Exception e) {
            log.error("Failed to notify producer about failed beat purchase {}: {}",
                    purchase.getId(), e.getMessage());
        }
    }

    private CreateNotificationRequest buildRequest(Integer userId, NotificationType type, String title,
                                                    String message, String relatedEntityId) {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setUserId(userId);
        request.setType(type);
        request.setTitle(title);
        request.setMessage(message);
        request.setRelatedEntityId(relatedEntityId);
        return request;
    }
}
