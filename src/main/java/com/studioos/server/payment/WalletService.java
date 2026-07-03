package com.studioos.server.payment;

import com.studioos.server.shared.enums.WalletType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;

    /**
     * Fetches a studio's wallet, creating one if it doesn't exist yet.
     * Called lazily so we don't need a listener on Studio creation.
     */
    @Transactional
    public Wallet getOrCreateStudioWallet(String studioId) {
        return walletRepository.findByStudioId(studioId)
                .orElseGet(() -> walletRepository.save(
                        Wallet.builder()
                                .type(WalletType.STUDIO)
                                .studioId(studioId)
                                .availableBalance(0)
                                .pendingBalance(0)
                                .withdrawnBalance(0)
                                .build()
                ));
    }

    /**
     * Fetches the single platform wallet, creating it if it doesn't exist yet.
     */
    @Transactional
    public Wallet getOrCreatePlatformWallet() {
        return walletRepository.findByType(WalletType.PLATFORM)
                .orElseGet(() -> walletRepository.save(
                        Wallet.builder()
                                .type(WalletType.PLATFORM)
                                .studioId(null)
                                .availableBalance(0)
                                .pendingBalance(0)
                                .withdrawnBalance(0)
                                .build()
                ));
    }

    /**
     * Moves money into pending balance (e.g. escrow held).
     */
    @Transactional
    public Wallet addToPending(String walletId, int amount) {
        Wallet wallet = getWalletOrThrow(walletId);
        wallet.setPendingBalance(wallet.getPendingBalance() + amount);
        return walletRepository.save(wallet);
    }

    /**
     * Releases funds from pending to available (e.g. booking delivered).
     */
    @Transactional
    public Wallet releasePendingToAvailable(String walletId, int amount) {
        Wallet wallet = getWalletOrThrow(walletId);
        if (wallet.getPendingBalance() < amount) {
            throw new IllegalStateException(
                    "Cannot release " + amount + " — pending balance is only " + wallet.getPendingBalance());
        }
        wallet.setPendingBalance(wallet.getPendingBalance() - amount);
        wallet.setAvailableBalance(wallet.getAvailableBalance() + amount);
        return walletRepository.save(wallet);
    }

    /**
     * Credits available balance directly (e.g. commission to platform wallet,
     * beat purchase payout — no pending stage involved).
     */
    @Transactional
    public Wallet creditAvailable(String walletId, int amount) {
        Wallet wallet = getWalletOrThrow(walletId);
        wallet.setAvailableBalance(wallet.getAvailableBalance() + amount);
        return walletRepository.save(wallet);
    }

    /**
     * Removes funds from pending balance without moving them to available
     * (e.g. producer-cancelled booking — money leaves escrow but producer never earns it).
     */
    @Transactional
    public Wallet removeFromPending(String walletId, int amount) {
        Wallet wallet = getWalletOrThrow(walletId);
        if (wallet.getPendingBalance() < amount) {
            throw new IllegalStateException(
                    "Cannot remove " + amount + " — pending balance is only " + wallet.getPendingBalance());
        }
        wallet.setPendingBalance(wallet.getPendingBalance() - amount);
        return walletRepository.save(wallet);
    }

    /**
     * Debits available balance and moves it to withdrawn (payout completed).
     */
    @Transactional
    public Wallet debitForWithdrawal(String walletId, int amount) {
        Wallet wallet = getWalletOrThrow(walletId);
        if (wallet.getAvailableBalance() < amount) {
            throw new IllegalStateException(
                    "Insufficient available balance: has " + wallet.getAvailableBalance() + ", needs " + amount);
        }
        wallet.setAvailableBalance(wallet.getAvailableBalance() - amount);
        wallet.setWithdrawnBalance(wallet.getWithdrawnBalance() + amount);
        return walletRepository.save(wallet);
    }

    private Wallet getWalletOrThrow(String walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId));
    }
}