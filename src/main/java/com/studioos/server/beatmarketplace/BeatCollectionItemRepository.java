package com.studioos.server.beatmarketplace;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BeatCollectionItemRepository extends JpaRepository<BeatCollectionItem, BeatCollectionItemId> {
    List<BeatCollectionItem> findByIdCollectionId(String collectionId);
}