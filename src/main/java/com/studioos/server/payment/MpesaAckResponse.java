package com.studioos.server.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MpesaAckResponse {
    private int resultCode;
    private String resultDesc;

    public static MpesaAckResponse ok() {
        return MpesaAckResponse.builder()
                .resultCode(0)
                .resultDesc("Accepted")
                .build();
    }
}