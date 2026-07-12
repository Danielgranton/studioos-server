package com.studioos.server.studio;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

public final class StudioSpecifications {

    private StudioSpecifications() {}

    public static Specification<Studio> locationContains(String location) {
        return (root, query, cb) -> {
            if (location == null || location.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + location.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("location")), pattern);
        };
    }

    public static Specification<Studio> pricingAtMost(Integer maxPrice) {
        return (root, query, cb) -> {
            if (maxPrice == null) {
                return cb.conjunction();
            }
            Predicate hasPrice = cb.isNotNull(root.get("pricing"));
            Predicate withinBudget = cb.lessThanOrEqualTo(root.get("pricing"), maxPrice);
            return cb.and(hasPrice, withinBudget);
        };
    }
}
