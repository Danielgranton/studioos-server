package com.studioos.server.user.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String bio;
    private String location;
    private String genre;
    private String experience;
    private String profileImage;
    private String instagram;
    private String youtube;
    private String link;
}