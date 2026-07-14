package com.studioos.server.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Double averageRating;
    private Integer reviewCount;
    private String createdAt;
}
