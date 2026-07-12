package com.studioos.server.reviews;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProducerReviewRepository extends JpaRepository<ProducerReview, String> {
    List<ProducerReview> findByProducerId(Integer producerId);
    Optional<ProducerReview> findByUserIdAndProducerId(Integer userId, Integer producerId);

    @Query("SELECT AVG(r.rating) FROM ProducerReview r WHERE r.producerId = :producerId")
    Double findAverageRatingByProducerId(Integer producerId);

    long countByProducerId(Integer producerId);
}
