package com.studioos.server.beatmarketplace.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BeatSaleResponse {
    private String id;
    private String beatId;
    private String beatTitle;
    private Integer amount;
    private String status;
    private boolean exclusive;
    private LocalDateTime purchasedAt;
}
