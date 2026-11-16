package com.riskregister.risk;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RiskRepository extends JpaRepository<Risk, Long>, JpaSpecificationExecutor<Risk> {

    @EntityGraph(attributePaths = "mitigations")
    List<Risk> findAll(Specification<Risk> spec);

    @EntityGraph(attributePaths = "mitigations")
    Optional<Risk> findWithMitigationsById(Long id);
}
