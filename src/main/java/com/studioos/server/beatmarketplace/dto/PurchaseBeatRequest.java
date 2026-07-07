package com.studioos.server.beatmarketplace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PurchaseBeatRequest {

    @NotBlank
    private String licenseId;

    @NotBlank
    private String phoneNumber;
}