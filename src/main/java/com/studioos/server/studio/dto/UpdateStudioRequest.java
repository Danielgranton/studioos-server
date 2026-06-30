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
    private String profileImage;
    private List<String> services;
}