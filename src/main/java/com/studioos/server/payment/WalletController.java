package com.studioos.server.payment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.studioos.server.payment.dto.WalletResponse;
import com.studioos.server.payment.dto.WalletWithdrawalRequest;
import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.user.User;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletController {
    private final ProducerWalletService producerWalletService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(producerWalletService.getWallet(user)));
    }

    @GetMapping("/studios/{studioId}")
    public ResponseEntity<ApiResponse<WalletResponse>> getStudioWallet(
            @AuthenticationPrincipal User user,
            @PathVariable String studioId) {
        return ResponseEntity.ok(ApiResponse.success(producerWalletService.getStudioWallet(user, studioId)));
    }

    @PostMapping("/studios/{studioId}/withdrawals")
    public ResponseEntity<ApiResponse<?>> requestWithdrawal(
            @AuthenticationPrincipal User user,
            @PathVariable String studioId,
            @Valid @RequestBody WalletWithdrawalRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Withdrawal request submitted",
                producerWalletService.requestWithdrawal(
                        user, studioId, request.getAmount(), request.getPhoneNumber())));
    }
}
