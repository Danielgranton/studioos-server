package com.studioos.server.beatmarketplace;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.studioos.server.shared.enums.BeatStatus;
import com.studioos.server.shared.enums.BeatVisibility;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;

public class BeatSpecifications {

    private BeatSpecifications() {}

    public static Specification<Beat> publicAndReady() {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), BeatStatus.READY),
                cb.equal(root.get("visibility"), BeatVisibility.PUBLIC)
        );
    }

    public static Specification<Beat> matchesCriteria(BeatSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getGenreId() != null) {
                predicates.add(cb.equal(root.get("genreId"), criteria.getGenreId()));
            }
            if (criteria.getMood() != null) {
                predicates.add(cb.equal(root.get("mood"), criteria.getMood()));
            }
            if (criteria.getBpmMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("bpm"), criteria.getBpmMin()));
            }
            if (criteria.getBpmMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("bpm"), criteria.getBpmMax()));
            }
            if (criteria.getKeySignature() != null) {
                predicates.add(cb.equal(root.get("keySignature"), criteria.getKeySignature()));
            }
            if (criteria.getProducerId() != null) {
                predicates.add(cb.equal(root.get("producerId"), criteria.getProducerId()));
            }
            if (criteria.getStudioId() != null) {
                predicates.add(cb.equal(root.get("studioId"), criteria.getStudioId()));
            }

            if (criteria.getPriceMin() != null || criteria.getPriceMax() != null) {
                Subquery<String> priceSubquery = query.subquery(String.class);
                var licenseRoot = priceSubquery.from(BeatLicense.class);
                priceSubquery.select(licenseRoot.get("beatId"));

                List<Predicate> licensePredicates = new ArrayList<>();
                licensePredicates.add(cb.isTrue(licenseRoot.get("active")));
                licensePredicates.add(cb.equal(licenseRoot.get("beatId"), root.get("id")));

                if (criteria.getPriceMin() != null) {
                    licensePredicates.add(cb.greaterThanOrEqualTo(licenseRoot.get("price"), criteria.getPriceMin()));
                }
                if (criteria.getPriceMax() != null) {
                    licensePredicates.add(cb.lessThanOrEqualTo(licenseRoot.get("price"), criteria.getPriceMax()));
                }

                priceSubquery.where(licensePredicates.toArray(new Predicate[0]));
                predicates.add(cb.exists(priceSubquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}