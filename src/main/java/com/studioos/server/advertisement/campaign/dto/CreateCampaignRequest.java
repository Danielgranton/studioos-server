package com.studioos.server.advertisement.campaign.dto;

import java.time.LocalDateTime;
import com.studioos.server.shared.enums.AdPlacement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CreateCampaignRequest {
    @NotBlank
    private String title;
    private String description;
    private String studioId;

    @NotNull
    private AdPlacement placement;

    @NotNull
    private LocalDateTime startDate;
    @NotNull
    private LocalDateTime endDate;

    @NotNull
    @Min(1)
    private Integer totalBudget;
    private Integer dailyBudget;
}