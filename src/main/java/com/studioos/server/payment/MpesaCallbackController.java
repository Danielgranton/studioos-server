package com.studioos.server.payment;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studioos.server.payment.dto.MpesaCallbackResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/payment/mpesa")
@RequiredArgsConstructor
public class MpesaCallbackController {

    private final MpesaService mpesaService;
    private final PaymentService paymentService;
    private final WithdrawalService withdrawalService;

    @PostMapping("/callback")
    public MpesaAckResponse handleStkCallback(@RequestBody String rawBody) {
        log.info("Received M-Pesa STK callback: {}", rawBody);
        handleAnyCallback(rawBody);
        return MpesaAckResponse.ok();
    }

    @PostMapping("/timeout")
    public MpesaAckResponse handleB2cResult(@RequestBody String rawBody) {
        log.info("Received M-Pesa B2C result/timeout callback: {}", rawBody);
        handleAnyCallback(rawBody);
        return MpesaAckResponse.ok();
    }

    private void handleAnyCallback(String rawBody) {
        try {
            MpesaCallbackResult stkResult = mpesaService.parseStkCallback(rawBody);
            if (stkResult.getReferenceId() != null && !stkResult.getReferenceId().isBlank()) {
                paymentService.handleMpesaCallback(
                        stkResult.getReferenceId(),
                        stkResult.isSuccess(),
                        stkResult.getMpesaReceiptNumber());
                return;
            }

            MpesaCallbackResult b2cResult = mpesaService.parseB2cCallback(rawBody);
            if (b2cResult.getReferenceId() != null && !b2cResult.getReferenceId().isBlank()) {
                withdrawalService.handleMpesaB2cCallback(
                        b2cResult.getReferenceId(),
                        b2cResult.isSuccess(),
                        b2cResult.getMpesaReceiptNumber());
                return;
            }

            log.warn("Ignoring M-Pesa callback with no recognizable reference id");
        } catch (Exception e) {
            log.error("Failed to process M-Pesa callback: {}", e.getMessage(), e);
        }
    }
}
