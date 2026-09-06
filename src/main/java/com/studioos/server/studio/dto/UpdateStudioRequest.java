package com.studioos.server.studio.dto;

import java.util.List;

import lombok.Data;

@Data
public class UpdateStudioRequest {
    private String studioName;
    private String location;
    private Integer pricing;
    private String availability;
    private String description;
    private String badge;
    private List<String> genres;
    private List<String> equipment;
    private Integer rooms;
    private Integer yearsActive;
    private String responseTime;
    private Boolean available;
    private String nextAvailable;
    private String profileImage;
    private List<String> services;
}
