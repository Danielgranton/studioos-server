package com.studioos.server.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.studioos.server.notification.NotificationServiceImpl;
import com.studioos.server.shared.enums.WithdrawalStatus;
import com.studioos.server.studio.StudioRepository;
import com.studioos.server.shared.enums.WalletType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    @Mock
    private WithdrawalRepository withdrawalRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private WalletService walletService;
    @Mock
    private StudioRepository studioRepository;
    @Mock
    private NotificationServiceImpl notificationService;
    @Mock
    private MpesaService mpesaService;

    @InjectMocks
    private WithdrawalService withdrawalService;

    @Test
    void approveWithdrawalReservesFundsBeforeInitiatingPayout() {
        Withdrawal withdrawal = Withdrawal.builder()
                .id("wd-1")
                .studioId("studio-1")
                .amount(5000)
                .status(WithdrawalStatus.PENDING)
                .mpesaPhoneNumber("+254700000000")
                .build();
        Wallet wallet = Wallet.builder()
                .id("wallet-1")
                .studioId("studio-1")
                .type(WalletType.STUDIO)
                .availableBalance(10000)
                .pendingBalance(0)
                .reservedBalance(0)
                .withdrawnBalance(0)
                .build();

        when(withdrawalRepository.findById("wd-1")).thenReturn(Optional.of(withdrawal));
        when(withdrawalRepository.save(any(Withdrawal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletService.getOrCreateStudioWallet("studio-1")).thenReturn(wallet);
        when(walletService.reserveForWithdrawal(anyString(), any(Integer.class))).thenAnswer(invocation -> {
            wallet.setAvailableBalance(wallet.getAvailableBalance() - 5000);
            wallet.setReservedBalance(wallet.getReservedBalance() + 5000);
            return wallet;
        });
        when(mpesaService.initiateB2cPayout(anyString(), any(Integer.class), anyString()))
                .thenReturn(new com.studioos.server.payment.dto.B2cInitiationResult(true, "conv-1", "orig-1", "Accepted"));

        Withdrawal approved = withdrawalService.approveWithdrawal("wd-1");

        assertThat(approved.getStatus()).isEqualTo(WithdrawalStatus.APPROVED);
        assertThat(wallet.getReservedBalance()).isEqualTo(5000);
        verify(mpesaService).initiateB2cPayout("+254700000000", 5000, "wd-1");
    }

    @Test
    void handleMpesaB2cCallbackCompletesWithdrawalAndCommitsReservedFunds() {
        Withdrawal withdrawal = Withdrawal.builder()
                .id("wd-1")
                .studioId("studio-1")
                .amount(5000)
                .status(WithdrawalStatus.APPROVED)
                .mpesaPhoneNumber("+254700000000")
                .build();
        Wallet wallet = Wallet.builder()
                .id("wallet-1")
                .studioId("studio-1")
                .type(WalletType.STUDIO)
                .availableBalance(5000)
                .pendingBalance(0)
                .reservedBalance(5000)
                .withdrawnBalance(0)
                .build();

        when(withdrawalRepository.findById("wd-1")).thenReturn(Optional.of(withdrawal));
        when(withdrawalRepository.save(any(Withdrawal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletService.getOrCreateStudioWallet("studio-1")).thenReturn(wallet);
        when(walletService.commitReservedWithdrawal(anyString(), any(Integer.class))).thenAnswer(invocation -> {
            wallet.setReservedBalance(wallet.getReservedBalance() - 5000);
            wallet.setWithdrawnBalance(wallet.getWithdrawnBalance() + 5000);
            return wallet;
        });
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Withdrawal completed = withdrawalService.handleMpesaB2cCallback("wd-1", true, "RCPT-1");

        assertThat(completed.getStatus()).isEqualTo(WithdrawalStatus.COMPLETED);
        assertThat(completed.getMpesaReceiptNumber()).isEqualTo("RCPT-1");
        assertThat(wallet.getReservedBalance()).isEqualTo(0);
        assertThat(wallet.getWithdrawnBalance()).isEqualTo(5000);
    }
}
