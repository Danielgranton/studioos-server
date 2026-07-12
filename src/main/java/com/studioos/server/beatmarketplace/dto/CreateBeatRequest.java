package com.studioos.server.beatmarketplace.dto;

import com.studioos.server.shared.enums.BeatVisibility;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    @Min(40)
    @Max(300)
    private Integer bpm;
    private String keySignature;
    private String mood;

    @NotNull
    private BeatVisibility visibility;

    @NotBlank
    private String studioId;
}
