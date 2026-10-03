package com.studioos.server.help;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface HelpArticleRepository extends JpaRepository<HelpArticle, String>, JpaSpecificationExecutor<HelpArticle> {
    Optional<HelpArticle> findBySlug(String slug);

    Page<HelpArticle> findByStatusOrderByFeaturedDescDisplayOrderAscPublishedAtDesc(
            HelpArticleStatus status,
            Pageable pageable);

    Page<HelpArticle> findByStatus(HelpArticleStatus status, Pageable pageable);
}
