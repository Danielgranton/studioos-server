package com.studioos.server.beatmarketplace;

import com.studioos.server.shared.enums.LicenseType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "beat_licenses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeatLicense {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String beatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "beatId", insertable = false, updatable = false)
    private Beat beat;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LicenseType type;

    @Column(nullable = false)
    private Integer price;

    @Builder.Default 
    private Boolean commercialUse = false;

    private Integer maxStreams;

    @Builder.Default
    private Boolean allowMusicVideo = false;

    @Builder.Default
    private Boolean allowRadio = false;

    @Column(name = "allow_tv")
    @Builder.Default
    private Boolean allowTV = false;

    @Builder.Default
    private Boolean allowModification = false;

    @Builder.Default
    private Boolean exclusive = false;

    @Builder.Default
    private Boolean active = true;
}