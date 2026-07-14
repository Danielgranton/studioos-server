package com.studioos.server.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudioDocument {
    private String id;
    private String studioName;
    private String location;
    private String description;
    private Integer pricing;
    private Integer ownerId;
    private Double averageRating;
    private Integer ratingCount;
}