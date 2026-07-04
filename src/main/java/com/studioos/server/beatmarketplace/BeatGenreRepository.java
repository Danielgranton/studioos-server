package com.studioos.server.beatmarketplace;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeatGenreRepository extends JpaRepository<BeatGenre, String> {
    Optional<BeatGenre> findByName(String name);
}