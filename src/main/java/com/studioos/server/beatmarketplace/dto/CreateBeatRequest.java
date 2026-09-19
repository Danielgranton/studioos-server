package com.studioos.server.beatmarketplace.dto;

import com.studioos.server.shared.enums.BeatVisibility;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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

    @Positive
    private Integer duration;

    @NotNull
    private BeatVisibility visibility;

    @NotBlank
    private String studioId;
}
