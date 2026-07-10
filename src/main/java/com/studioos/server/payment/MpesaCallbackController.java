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

    @PostMapping("/callback")
    public MpesaAckResponse handleStkCallback(@RequestBody String rawBody) {
        log.info("Received M-Pesa STK callback: {}", rawBody);

        try {
            MpesaCallbackResult result = mpesaService.parseStkCallback(rawBody);
            paymentService.handleMpesaCallback(result.getReferenceId(), result.isSuccess(), result.getMpesaReceiptNumber());
        } catch (Exception e) {
            log.error("Failed to process STK callback: {}", e.getMessage(), e);
        }

        return MpesaAckResponse.ok();
    }

    @PostMapping("/timeout")
    public MpesaAckResponse handleB2cResult(@RequestBody String rawBody) {
        log.info("Received M-Pesa B2C result/timeout callback: {}", rawBody);
     
        try {
            MpesaCallbackResult result = mpesaService.parseB2cCallback(rawBody);
            log.info("B2C result parsed (not yet persisted): success={}, amount={}, referenceId={}",
                result.isSuccess(), result.getAmount(),result.getReferenceId()
             );
        } catch (Exception e) {
            log.error("Failed to process B2C callback: {}", e.getMessage(),e);
        }

        return MpesaAckResponse.ok();
    }
}