package com.studioos.server.artist;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistServiceRequestRepository extends JpaRepository<ArtistServiceRequest, String> {
    List<ArtistServiceRequest> findByArtistIdOrderByCreatedAtDesc(Integer artistId);
    Optional<ArtistServiceRequest> findByIdAndArtistId(String id, Integer artistId);
}
