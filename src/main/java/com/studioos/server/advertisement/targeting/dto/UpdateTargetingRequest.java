package com.studioos.server.advertisement.targeting.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class UpdateTargetingRequest {
    private String countries;
    private String cities;
    private String genres;

    @PositiveOrZero(message = "ageMin must be zero or greater")
    private Integer ageMin;

    @PositiveOrZero(message = "ageMax must be zero or greater")
    private Integer ageMax;

    private String gender;
    private String interests;
    private String devices;
}
