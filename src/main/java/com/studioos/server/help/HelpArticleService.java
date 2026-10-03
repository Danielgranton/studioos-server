package com.studioos.server.help;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studioos.server.help.dto.HelpArticleResponse;
import com.studioos.server.help.dto.HelpArticleAdminResponse;
import com.studioos.server.help.dto.HelpArticleSummaryResponse;
import com.studioos.server.help.dto.HelpCategoryResponse;
import com.studioos.server.help.dto.HelpArticleUpsertRequest;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HelpArticleService {

    private final HelpCategoryRepository categoryRepository;
    private final HelpArticleRepository articleRepository;

    @Transactional(readOnly = true)
    public List<HelpCategoryResponse> getCategories() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc().stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<HelpArticleAdminResponse> getAdminArticles(Pageable pageable) {
        return articleRepository.findAll(pageable).map(this::toAdminResponse);
    }

    @Transactional
    public HelpArticleAdminResponse createArticle(User admin, HelpArticleUpsertRequest request) {
        if (articleRepository.findBySlug(request.slug()).isPresent()) {
            throw StudioosException.conflict("A help article with this slug already exists");
        }

        HelpArticle article = new HelpArticle();
        article.setCreatedBy(admin.getId());
        applyRequest(article, request);
        article.setStatus(HelpArticleStatus.DRAFT);
        return toAdminResponse(articleRepository.save(article));
    }

    @Transactional
    public HelpArticleAdminResponse updateArticle(String articleId, HelpArticleUpsertRequest request) {
        HelpArticle article = getAdminArticle(articleId);
        articleRepository.findBySlug(request.slug()).filter(existing -> !existing.getId().equals(articleId)).ifPresent(existing -> {
            throw StudioosException.conflict("A help article with this slug already exists");
        });
        applyRequest(article, request);
        return toAdminResponse(article);
    }

    @Transactional
    public HelpArticleAdminResponse publishArticle(String articleId) {
        HelpArticle article = getAdminArticle(articleId);
        article.setStatus(HelpArticleStatus.PUBLISHED);
        article.setPublishedAt(article.getPublishedAt() == null ? LocalDateTime.now() : article.getPublishedAt());
        return toAdminResponse(article);
    }

    @Transactional
    public HelpArticleAdminResponse archiveArticle(String articleId) {
        HelpArticle article = getAdminArticle(articleId);
        article.setStatus(HelpArticleStatus.ARCHIVED);
        return toAdminResponse(article);
    }

    @Transactional(readOnly = true)
    public Page<HelpArticleSummaryResponse> search(
            String query,
            String categorySlug,
            HelpAudience audience,
            int page,
            int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Order.desc("featured"), Sort.Order.asc("displayOrder"), Sort.Order.desc("publishedAt")));

        Specification<HelpArticle> specification = publicArticleSpecification(query, categorySlug, audience);
        return articleRepository.findAll(specification, pageable).map(this::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public Page<HelpArticleSummaryResponse> getPopular(int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Order.desc("viewCount"), Sort.Order.desc("helpfulCount"), Sort.Order.desc("publishedAt")));
        return articleRepository.findByStatus(HelpArticleStatus.PUBLISHED, pageable)
                .map(this::toSummaryResponse);
    }

    @Transactional
    public HelpArticleResponse getBySlug(String slug) {
        HelpArticle article = articleRepository.findOne(publicArticleSpecification(null, null, null)
                        .and((root, query, cb) -> cb.equal(root.get("slug"), slug)))
                .orElseThrow(() -> StudioosException.notFound("Help article not found"));
        article.setViewCount(article.getViewCount() + 1);
        return toArticleResponse(article);
    }

    @Transactional
    public void recordFeedback(String slug, Boolean helpful) {
        if (helpful == null) {
            throw StudioosException.badRequest("Feedback must specify whether the article was helpful");
        }

        HelpArticle article = articleRepository.findOne(publicArticleSpecification(null, null, null)
                        .and((root, query, cb) -> cb.equal(root.get("slug"), slug)))
                .orElseThrow(() -> StudioosException.notFound("Help article not found"));

        if (helpful) {
            article.setHelpfulCount(article.getHelpfulCount() + 1);
        } else {
            article.setNotHelpfulCount(article.getNotHelpfulCount() + 1);
        }
    }

    private Specification<HelpArticle> publicArticleSpecification(
            String query,
            String categorySlug,
            HelpAudience audience) {
        return (root, criteriaQuery, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("status"), HelpArticleStatus.PUBLISHED));

            if (categorySlug != null && !categorySlug.isBlank()) {
                predicates.add(cb.equal(root.join("category").get("slug"), categorySlug.trim().toLowerCase(Locale.ROOT)));
            }
            if (audience != null && audience != HelpAudience.ALL) {
                predicates.add(cb.or(
                        cb.equal(root.get("audience"), HelpAudience.ALL),
                        cb.equal(root.get("audience"), audience)));
            }
            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("excerpt")), pattern),
                        cb.like(cb.lower(root.get("content")), pattern)));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private HelpArticle getAdminArticle(String articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> StudioosException.notFound("Help article not found"));
    }

    private void applyRequest(HelpArticle article, HelpArticleUpsertRequest request) {
        HelpCategory category = categoryRepository.findBySlugAndActiveTrue(request.categorySlug().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> StudioosException.badRequest("Help category not found"));
        article.setSlug(request.slug().trim());
        article.setTitle(request.title().trim());
        article.setExcerpt(request.excerpt().trim());
        article.setContent(request.content().trim());
        article.setCategory(category);
        article.setAudience(request.audience());
        article.setFeatured(Boolean.TRUE.equals(request.featured()));
        article.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
        article.setReadTimeMinutes(request.readTimeMinutes() == null ? 1 : request.readTimeMinutes());
        article.setTags(request.tags() == null ? new java.util.HashSet<>() : new java.util.HashSet<>(request.tags()));
    }

    private HelpCategoryResponse toCategoryResponse(HelpCategory category) {
        return HelpCategoryResponse.builder()
                .id(category.getId())
                .slug(category.getSlug())
                .name(category.getName())
                .description(category.getDescription())
                .displayOrder(category.getDisplayOrder())
                .build();
    }

    private HelpArticleResponse toArticleResponse(HelpArticle article) {
        return HelpArticleResponse.builder()
                .id(article.getId())
                .slug(article.getSlug())
                .title(article.getTitle())
                .excerpt(article.getExcerpt())
                .content(article.getContent())
                .categorySlug(article.getCategory().getSlug())
                .categoryName(article.getCategory().getName())
                .audience(article.getAudience())
                .readTimeMinutes(article.getReadTimeMinutes())
                .featured(article.getFeatured())
                .viewCount(article.getViewCount())
                .publishedAt(article.getPublishedAt())
                .tags(Set.copyOf(article.getTags()))
                .relatedArticles(article.getRelatedArticles().stream().map(this::toSummaryResponse).toList())
                .build();
    }

    private HelpArticleSummaryResponse toSummaryResponse(HelpArticle article) {
        return HelpArticleSummaryResponse.builder()
                .id(article.getId())
                .slug(article.getSlug())
                .title(article.getTitle())
                .excerpt(article.getExcerpt())
                .categorySlug(article.getCategory().getSlug())
                .categoryName(article.getCategory().getName())
                .audience(article.getAudience())
                .readTimeMinutes(article.getReadTimeMinutes())
                .featured(article.getFeatured())
                .viewCount(article.getViewCount())
                .publishedAt(article.getPublishedAt())
                .tags(Set.copyOf(article.getTags()))
                .build();
    }

    private HelpArticleAdminResponse toAdminResponse(HelpArticle article) {
        return new HelpArticleAdminResponse(
                article.getId(), article.getSlug(), article.getTitle(), article.getExcerpt(), article.getContent(),
                article.getCategory().getSlug(), article.getCategory().getName(), article.getStatus(), article.getAudience(),
                article.getFeatured(), article.getDisplayOrder(), article.getReadTimeMinutes(), article.getViewCount(),
                article.getHelpfulCount(), article.getNotHelpfulCount(), article.getPublishedAt(), Set.copyOf(article.getTags()),
                article.getUpdatedAt());
    }
}
