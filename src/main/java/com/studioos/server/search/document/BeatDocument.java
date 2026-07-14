package com.studioos.server.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeatDocument {
    private String id;
    private String title;
    private String genre;
    private Integer bpm;
    private String keySignature;
    private String mood;
    private Integer producerId;
    private String studioId;
    private Integer price;
    private Integer playCount;
    private Integer likeCount;
    private String status;
    private String createdAt;  
}