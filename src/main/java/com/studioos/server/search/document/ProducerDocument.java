package com.studioos.server.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProducerDocument {
    private Integer id;
    private String name;
    private String location;
    private String genre;
    private String bio;
    private String profileImage;
    private String profileImageThumbnail;
    private Boolean verified;
    private List<String> studioNames;
    private Integer studioCount;
    private Boolean available;
    private Integer startingPrice;
    private String responseTime;
    private List<String> services;
    private Double averageRating;
    private Integer reviewCount;
    private String createdAt;
}
