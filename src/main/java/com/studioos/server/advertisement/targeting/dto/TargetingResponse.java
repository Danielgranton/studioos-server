package com.studioos.server.advertisement.targeting.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TargetingResponse {
    private String campaignId;
    private String countries;
    private String cities;
    private String genres;
    private Integer ageMin;
    private Integer ageMax;
    private String gender;
    private String interests;
    private String devices;
}
