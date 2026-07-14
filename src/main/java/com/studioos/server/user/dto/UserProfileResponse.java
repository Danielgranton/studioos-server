package com.studioos.server.user.dto;

import com.studioos.server.shared.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Integer id;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private String bio;
    private String location;
    private String genre;
    private String experience;
    private String profileImage;
    private String profileImageLarge;
    private String profileImageMedium;
    private String profileImageThumbnail;
    private String instagram;
    private String youtube;
    private String link;
    private LocalDateTime createdAt;
}
