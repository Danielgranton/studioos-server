package com.studioos.server.beatmarketplace;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.studioos.server.shared.enums.BeatPaymentStatus;

public interface BeatPurchaseRepository extends JpaRepository<BeatPurchase, String> {
    List<BeatPurchase> findByBuyerId(Integer buyerId);
    List<BeatPurchase> findByBeatId(String beatId);
    boolean existsByBeatIdAndBuyerIdAndStatus(String beatId, Integer buyerId, BeatPaymentStatus status);
    Optional<BeatPurchase> findByTransactionId(String transactionId);
    Optional<BeatPurchase> findByBeatIdAndBuyerIdAndStatus(String beatId, Integer buyerId, BeatPaymentStatus status);
    List<BeatPurchase> findByLicenseIdAndStatusIn(String licenseId, List<BeatPaymentStatus> statuses);
    List<BeatPurchase> findByBeatIdInAndStatus(List<String> beatIds, BeatPaymentStatus status);
}