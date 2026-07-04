package com.studioos.server.beatmarketplace.dto;

import com.studioos.server.shared.enums.BeatVisibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateBeatRequest {

    @NotBlank
    private String title;

    private String description;

    @NotBlank
    private String genreId;

    private Integer bpm;
    private String keySignature;
    private String mood;

    @NotNull
    private BeatVisibility visibility;

    private String studioId;
}