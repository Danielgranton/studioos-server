package com.studioos.server.artist.dto;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ArtistBrowseResponse {
    Integer id;
    String name;
    String username;
    String location;
    String genre;
    String bio;
    String experience;
    String profileImage;
    String profileImageLarge;
    String profileImageMedium;
    String profileImageThumbnail;
    boolean verified;
    List<ArtistServiceOfferingResponse> services;
}
