package com.studioos.server.studio;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StudioRepository extends JpaRepository<Studio, String>, JpaSpecificationExecutor<Studio> {

    @EntityGraph(attributePaths = "services")
    List<Studio> findByOwnerId(Integer ownerId);

    Page<Studio> findAll(Pageable pageable);

    @Query("SELECT COUNT(s) FROM StudioService s")
    long countServiceOfferings();

    @Query("SELECT s FROM Studio s LEFT JOIN s.ratings r GROUP BY s ORDER BY COALESCE(AVG(r.rating), 0) DESC, COUNT(r.id) DESC, s.createdAt DESC")
    Page<Studio> findFeatured(Pageable pageable);

    Page<Studio> findByAvailableTrue(Pageable pageable);

    Page<Studio> findByPricingGreaterThanEqual(Integer minimumPrice, Pageable pageable);

    Page<Studio> findByPricingLessThanEqual(Integer maximumPrice, Pageable pageable);

    @Query("SELECT DISTINCT s FROM Studio s JOIN s.services service WHERE LOWER(service.name) LIKE LOWER(CONCAT('%', :service, '%'))")
    Page<Studio> findByService(String service, Pageable pageable);
}
