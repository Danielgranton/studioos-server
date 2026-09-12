package com.studioos.server.artist;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistServiceOfferingRepository extends JpaRepository<ArtistServiceOffering, String> {
    List<ArtistServiceOffering> findByArtistIdAndActiveTrueOrderByCreatedAtDesc(Integer artistId);
    List<ArtistServiceOffering> findByArtistIdOrderByCreatedAtDesc(Integer artistId);
    Optional<ArtistServiceOffering> findByIdAndArtistId(String id, Integer artistId);
}
