package com.studioos.server.beatmarketplace;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.beatmarketplace.dto.BeatLicenseResponse;
import com.studioos.server.beatmarketplace.dto.CreateLicenseRequest;
import com.studioos.server.beatmarketplace.dto.CreateLicensesRequest;
import com.studioos.server.shared.enums.LicenseType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BeatLicenseService {

    private final BeatRepository beatRepository;
    private final BeatLicenseRepository beatLicenseRepository;

    @Transactional
    public List<BeatLicenseResponse> createLicenses(Integer producerId, String beatId, CreateLicensesRequest request) {

        Beat beat = beatRepository.findById(beatId)
                .orElseThrow(() -> new IllegalArgumentException("Beat not found: " + beatId));

        if (!beat.getProducerId().equals(producerId)) {
            throw new SecurityException("Producer does not own this beat");
        }

        return request.getLicenses().stream()
                .map(req -> createSingleLicense(beat, req))
                .collect(Collectors.toList());
    }

    private BeatLicenseResponse createSingleLicense(Beat beat, CreateLicenseRequest req) {

        boolean isExclusive = req.getType() == LicenseType.EXCLUSIVE;

        if (isExclusive && Boolean.TRUE.equals(beat.getExclusiveSold())) {
            throw new IllegalStateException(
                    "Cannot create an exclusive license — beat " + beat.getId() + " has already been sold exclusively");
        }

        beatLicenseRepository.findByBeatIdAndTypeAndActiveTrue(beat.getId(), req.getType())
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "An active " + req.getType() + " license already exists for beat " + beat.getId()
                                    + " — deactivate it before creating a new one");
                });

        BeatLicense license = BeatLicense.builder()
                .beatId(beat.getId())
                .type(req.getType())
                .price(req.getPrice())
                .commercialUse(req.isCommercialUse())
                .maxStreams(req.getMaxStreams())
                .allowMusicVideo(req.isAllowMusicVideo())
                .allowRadio(req.isAllowRadio())
                .allowTV(req.isAllowTV())
                .allowModification(req.isAllowModification())
                .exclusive(isExclusive)
                .active(true)
                .build();

        license = beatLicenseRepository.save(license);

        return toResponse(license);
    }

    public List<BeatLicenseResponse> getLicensesForBeat(String beatId) {
        return beatLicenseRepository.findByBeatIdAndActiveTrue(beatId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private BeatLicenseResponse toResponse(BeatLicense license) {
        return BeatLicenseResponse.builder()
                .id(license.getId())
                .beatId(license.getBeatId())
                .type(license.getType())
                .price(license.getPrice())
                .commercialUse(license.getCommercialUse())
                .maxStreams(license.getMaxStreams())
                .allowMusicVideo(license.getAllowMusicVideo())
                .allowRadio(license.getAllowRadio())
                .allowTV(license.getAllowTV())
                .allowModification(license.getAllowModification())
                .exclusive(license.getExclusive())
                .active(license.getActive())
                .build();
    }
}