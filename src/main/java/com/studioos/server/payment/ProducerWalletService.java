package com.studioos.server.payment;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.payment.dto.WalletResponse;
import com.studioos.server.payment.dto.WalletStudioResponse;
import com.studioos.server.payment.dto.WalletTransactionResponse;
import com.studioos.server.payment.dto.WalletWithdrawalResponse;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.studio.Studio;
import com.studioos.server.studio.StudioRepository;
import com.studioos.server.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProducerWalletService {
    private final StudioRepository studioRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final WithdrawalRepository withdrawalRepository;
    private final WithdrawalService withdrawalService;

    @Transactional(readOnly = true)
    public WalletResponse getWallet(User user) {
        requireProducer(user);
        List<Studio> studios = studioRepository.findByOwnerId(user.getId());
        return buildResponse(studios);
    }

    @Transactional(readOnly = true)
    public WalletResponse getStudioWallet(User user, String studioId) {
        Studio studio = findOwnedStudio(user, studioId);
        return buildResponse(List.of(studio));
    }

    @Transactional
    public WalletWithdrawalResponse requestWithdrawal(User user, String studioId, int amount, String phoneNumber) {
        findOwnedStudio(user, studioId);
        return toWithdrawal(withdrawalService.requestWithdrawal(studioId, amount, phoneNumber));
    }

    private WalletResponse buildResponse(List<Studio> studios) {
        List<WalletStudioResponse> studioWallets = new ArrayList<>();
        List<WalletTransactionResponse> transactions = new ArrayList<>();
        List<WalletWithdrawalResponse> withdrawals = new ArrayList<>();
        int available = 0;
        int pending = 0;
        int reserved = 0;
        int withdrawn = 0;

        for (Studio studio : studios) {
            Wallet wallet = walletRepository.findByStudioId(studio.getId()).orElse(null);
            int studioAvailable = wallet == null ? 0 : zero(wallet.getAvailableBalance());
            int studioPending = wallet == null ? 0 : zero(wallet.getPendingBalance());
            int studioReserved = wallet == null ? 0 : zero(wallet.getReservedBalance());
            int studioWithdrawn = wallet == null ? 0 : zero(wallet.getWithdrawnBalance());
            available += studioAvailable;
            pending += studioPending;
            reserved += studioReserved;
            withdrawn += studioWithdrawn;
            studioWallets.add(WalletStudioResponse.builder()
                    .studioId(studio.getId())
                    .studioName(studio.getStudioName())
                    .availableBalance(studioAvailable)
                    .pendingBalance(studioPending)
                    .reservedBalance(studioReserved)
                    .withdrawnBalance(studioWithdrawn)
                    .build());
            transactionRepository.findByStudioIdOrderByCreatedAtDesc(studio.getId())
                    .stream().map(this::toTransaction).forEach(transactions::add);
            withdrawalRepository.findByStudioIdOrderByCreatedAtDesc(studio.getId())
                    .stream().map(this::toWithdrawal).forEach(withdrawals::add);
        }

        return WalletResponse.builder()
                .availableBalance(available)
                .pendingBalance(pending)
                .reservedBalance(reserved)
                .withdrawnBalance(withdrawn)
                .studios(studioWallets)
                .transactions(transactions.stream().limit(30).toList())
                .withdrawals(withdrawals.stream().limit(30).toList())
                .build();
    }

    private Studio findOwnedStudio(User user, String studioId) {
        Studio studio = studioRepository.findById(studioId)
                .orElseThrow(() -> StudioosException.notFound("Studio not found"));
        if (!studio.getOwnerId().equals(user.getId()) && user.getRole() != Role.SUPER_ADMIN) {
            throw StudioosException.forbidden("You do not own this studio wallet");
        }
        return studio;
    }

    private void requireProducer(User user) {
        if (user.getRole() != Role.PRODUCER && user.getRole() != Role.SUPER_ADMIN) {
            throw StudioosException.forbidden("Only producers can access studio wallets");
        }
    }

    private WalletTransactionResponse toTransaction(Transaction transaction) {
        return WalletTransactionResponse.builder()
                .id(transaction.getId())
                .studioId(transaction.getStudioId())
                .type(transaction.getType().name())
                .status(transaction.getStatus().name())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .mpesaReceiptNumber(transaction.getMpesaReceiptNumber())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    private WalletWithdrawalResponse toWithdrawal(Withdrawal withdrawal) {
        return WalletWithdrawalResponse.builder()
                .id(withdrawal.getId())
                .studioId(withdrawal.getStudioId())
                .amount(withdrawal.getAmount())
                .status(withdrawal.getStatus().name())
                .mpesaPhoneNumber(withdrawal.getMpesaPhoneNumber())
                .mpesaReceiptNumber(withdrawal.getMpesaReceiptNumber())
                .rejectionReason(withdrawal.getRejectionReason())
                .createdAt(withdrawal.getCreatedAt())
                .build();
    }

    private int zero(Integer value) {
        return value == null ? 0 : value;
    }
}
