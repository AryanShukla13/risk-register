package com.riskregister.mitigation;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MitigationRepository extends JpaRepository<Mitigation, Long> {

    Optional<Mitigation> findByIdAndRiskId(Long id, Long riskId);

    long countByRiskId(Long riskId);
}
