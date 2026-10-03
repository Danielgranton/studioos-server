package com.studioos.server.servicecatalog;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalog, String> {
    List<ServiceCatalog> findByActiveTrueOrderByCategoryAscNameAsc();
    List<ServiceCatalog> findByActiveTrueAndArtistAllowedTrueOrderByCategoryAscNameAsc();
    List<ServiceCatalog> findByActiveTrueAndStudioAllowedTrueOrderByCategoryAscNameAsc();
    Optional<ServiceCatalog> findBySlugAndActiveTrue(String slug);
    Optional<ServiceCatalog> findByNameIgnoreCase(String name);
}
