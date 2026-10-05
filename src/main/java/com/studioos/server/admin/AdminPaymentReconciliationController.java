package com.studioos.server.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.payment.PaymentService;
import com.studioos.server.payment.TransactionRepository;
import com.studioos.server.payment.dto.MpesaReviewCaseResponse;
import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.shared.enums.TransactionStatus;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/payments/reconciliation")
@RequiredArgsConstructor
public class AdminPaymentReconciliationController {
    private final TransactionRepository transactionRepository;
    private final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<MpesaReviewCaseResponse>>> pendingReviewCases(
            @PageableDefault(size = 25, sort = "mpesaReviewFlaggedAt", direction = Sort.Direction.ASC)
            Pageable pageable) {
        Page<MpesaReviewCaseResponse> cases = transactionRepository
                .findByMpesaReviewRequiredTrueAndStatusOrderByMpesaReviewFlaggedAtAsc(
                        TransactionStatus.PENDING, pageable)
                .map(MpesaReviewCaseResponse::from);
        return ResponseEntity.ok(ApiResponse.success(cases));
    }

    @PostMapping("/{transactionId}/check")
    public ResponseEntity<ApiResponse<MpesaReviewCaseResponse>> checkStatus(
            @PathVariable String transactionId) {
        paymentService.reconcileStaleTransaction(transactionId);
        var transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));
        return ResponseEntity.ok(ApiResponse.success(
                "Daraja status checked; unresolved payments remain reserved", MpesaReviewCaseResponse.from(transaction)));
    }
}
