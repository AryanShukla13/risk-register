package com.riskregister.risk;

import org.springframework.data.jpa.domain.Specification;

final class RiskSpecifications {

    private RiskSpecifications() {}

    /** Both filters are optional; null means "no restriction". */
    static Specification<Risk> matching(RiskCategory category, RiskStatus status) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (category != null) predicates.add(cb.equal(root.get("category"), category));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
