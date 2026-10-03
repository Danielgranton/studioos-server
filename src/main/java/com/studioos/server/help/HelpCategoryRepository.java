package com.studioos.server.help;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HelpCategoryRepository extends JpaRepository<HelpCategory, String> {
    List<HelpCategory> findByActiveTrueOrderByDisplayOrderAscNameAsc();
    Optional<HelpCategory> findBySlugAndActiveTrue(String slug);
}
