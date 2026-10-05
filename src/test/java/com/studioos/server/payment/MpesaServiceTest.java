package com.studioos.server.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.studioos.server.payment.dto.MpesaCallbackResult;

import org.junit.jupiter.api.Test;

class MpesaServiceTest {

    @Test
    void rejectsLocalHttpCallbackUrlsBeforeCallingDaraja() {
        assertThat(MpesaService.callbackUrlProblem("http://localhost:8080/api/v1/payment/mpesa/callback", "MPESA_CALLBACK_URL"))
                .contains("public HTTPS URL");
        assertThat(MpesaService.callbackUrlProblem("https://127.0.0.1/payment/callback", "MPESA_CALLBACK_URL"))
                .contains("public HTTPS URL");
    }

    @Test
    void acceptsPublicHttpsCallbackUrl() {
        assertThat(MpesaService.callbackUrlProblem("https://studioos.example/api/v1/payment/mpesa/callback", "MPESA_CALLBACK_URL"))
                .isNull();
    }

    @Test
    void normalizesCommonKenyanMobileFormatsToSafaricomMsisdn() {
        assertThat(MpesaPhoneNumber.normalize("+254 712 345 678")).isEqualTo("254712345678");
        assertThat(MpesaPhoneNumber.normalize("0712-345-678")).isEqualTo("254712345678");
        assertThat(MpesaPhoneNumber.normalize("112345678")).isEqualTo("254112345678");
    }

    @Test
    void rejectsNonKenyanOrMalformedPhoneNumbers() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> MpesaPhoneNumber.normalize("+1 555 010 1234"))
                .isInstanceOf(RuntimeException.class);
    }

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
