package com.studioos.server.studio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StudioLikeRepository extends JpaRepository<StudioLike, String> {
    Optional<StudioLike> findByUserIdAndStudioId(Integer userId, String studioId);
    long countByStudioId(String studioId);
}
