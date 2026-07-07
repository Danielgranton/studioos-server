package com.studioos.server.beatmarketplace;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.studioos.server.shared.enums.LicenseType;

public interface BeatLicenseRepository extends JpaRepository<BeatLicense, String> {
    List<BeatLicense> findByBeatId(String beatId);
    List<BeatLicense> findByBeatIdAndActiveTrue(String beatId);
    Optional<BeatLicense> findByBeatIdAndTypeAndActiveTrue(String beatId, LicenseType type);

    @Query("SELECT bl.beatId AS beatId, MIN(bl.price) AS minPrice " +
           "FROM BeatLicense bl WHERE bl.active = true AND bl.beatId IN :beatIds " +
           "GROUP BY bl.beatId")
    List<BeatMinPriceProjection> findMinPricesByBeatIds(@Param("beatIds") List<String> beatIds);
}