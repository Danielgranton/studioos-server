package com.studioos.server.beatmarketplace;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeatTagRepository extends JpaRepository<BeatTag, String> {
    Optional<BeatTag> findByName(String name);
}