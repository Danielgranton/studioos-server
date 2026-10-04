package com.studioos.server.artist;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtistServiceOfferingRepository extends JpaRepository<ArtistServiceOffering, String> {
    List<ArtistServiceOffering> findByArtistIdAndActiveTrueOrderByCreatedAtDesc(Integer artistId);
    List<ArtistServiceOffering> findByArtistIdOrderByCreatedAtDesc(Integer artistId);
    Optional<ArtistServiceOffering> findByIdAndArtistId(String id, Integer artistId);
    List<ArtistServiceOffering> findByCatalogServiceIdAndActiveTrueOrderByCreatedAtDesc(String catalogServiceId);

    @Query("SELECT offering FROM ArtistServiceOffering offering WHERE offering.active = true AND LOWER(TRIM(offering.name)) IN :normalizedNames ORDER BY offering.createdAt DESC")
    List<ArtistServiceOffering> findActiveByNormalizedNames(@Param("normalizedNames") Collection<String> normalizedNames);
}
