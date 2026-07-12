package com.studioos.server.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.studioos.server.payment.dto.MpesaCallbackResult;

import org.junit.jupiter.api.Test;

class MpesaServiceTest {

    @Test
    void parseStkCallbackKeepsCheckoutRequestIdOnFailure() {
        MpesaService service = new MpesaService(mock(MpesaProperties.class));

        MpesaCallbackResult result = service.parseStkCallback("""
                {
                  "Body": {
                    "stkCallback": {
                      "MerchantRequestID": "m-1",
                      "CheckoutRequestID": "c-1",
                      "ResultCode": 1,
                      "ResultDesc": "Rejected"
                    }
                  }
                }
                """);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getReferenceId()).isEqualTo("c-1");
    }

    @Test
    void parseB2cCallbackKeepsOccasionOnFailure() {
        MpesaService service = new MpesaService(mock(MpesaProperties.class));

        MpesaCallbackResult result = service.parseB2cCallback("""
                {
                  "Result": {
                    "ResultType": 0,
                    "ResultCode": 2001,
                    "ResultDesc": "Insufficient Funds",
                    "Occasion": "wd-1"
                  }
                }
                """);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getReferenceId()).isEqualTo("wd-1");
    }
}
