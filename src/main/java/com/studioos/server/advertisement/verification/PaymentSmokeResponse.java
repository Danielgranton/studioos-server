package com.studioos.server.advertisement.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PaymentSmokeResponse {
    private boolean accepted;
    private String responseDescription;
    private String merchantRequestId;
    private String checkoutRequestId;
}
