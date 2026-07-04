package com.studioos.server.beatmarketplace;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BeatLicenseRepository extends JpaRepository<BeatLicense, String> {
    List<BeatLicense> findByBeatId(String beatId);
    List<BeatLicense> findByBeatIdAndActiveTrue(String beatId);
}