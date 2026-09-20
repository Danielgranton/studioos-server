package com.studioos.server.beatmarketplace.dto;

import com.studioos.server.shared.enums.VerificationStatus;
import com.studioos.server.shared.enums.LicenseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class BeatSummaryResponse {
    private String id;
    private String title;
    private String description;
    private String mood;
    private String studioId;
    private String genreId;
    private String coverUrl;
    private String thumbnailUrl;
    private String genreName;
    private Integer bpm;
    private String keySignature;
    private Integer startingPrice;   // cheapest active license price, null if no licenses yet
    private LicenseType licenseType;
    private Integer likeCount;
    private Integer playCount;
    private Double averageRating;
    private Long reviewCount;
    private String producerId;
    private String producerName;
    private Integer duration;
    private boolean exclusive;
    private boolean verified;
    private VerificationStatus verificationStatus;
    private String status;
    private String visibility;
    private String waveformUrl;
    private boolean previewAvailable;
}
