package com.studioos.server.studio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StudioMediaRepository extends JpaRepository<StudioMedia, String> {
    List<StudioMedia> findByStudioIdOrderByDisplayOrderAscCreatedAtAsc(String studioId);
    long countByStudioIdAndMediaType(String studioId, String mediaType);
}
