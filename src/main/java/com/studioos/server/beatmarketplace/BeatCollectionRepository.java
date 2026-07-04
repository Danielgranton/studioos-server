package com.studioos.server.beatmarketplace;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BeatCollectionRepository extends JpaRepository<BeatCollection, String> {
    List<BeatCollection> findByUserId(Integer userId);
}