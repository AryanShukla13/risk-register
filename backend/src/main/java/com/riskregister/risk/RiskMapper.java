package com.riskregister.risk;

import com.riskregister.mitigation.Mitigation;
import com.riskregister.mitigation.MitigationMapper;
import com.riskregister.risk.dto.RiskResponse;
import com.riskregister.risk.dto.RiskSummaryResponse;
import com.riskregister.scoring.ResidualScoreCalculator;
import com.riskregister.scoring.RiskScoring;
import com.riskregister.scoring.SeverityBand;
import java.util.List;
import org.springframework.stereotype.Component;

/** Entity to DTO mapping, including the derived (never stored) scores. */
@Component
public class RiskMapper {

    private final ResidualScoreCalculator calculator;

    public RiskMapper(ResidualScoreCalculator calculator) {
        this.calculator = calculator;
    }

    public RiskResponse toResponse(Risk r) {
        int inherent = RiskScoring.inherent(r.getLikelihood(), r.getImpact());
        int residual = residualOf(r, inherent);
        return new RiskResponse(r.getId(), r.getTitle(), r.getDescription(), r.getCategory(), r.getOwner(),
                r.getLikelihood(), r.getImpact(), r.getStatus(),
                inherent, SeverityBand.of(inherent), residual, SeverityBand.of(residual),
                r.getMitigations().stream().map(MitigationMapper::toResponse).toList(),
                r.getCreatedAt(), r.getUpdatedAt());
    }

    public RiskSummaryResponse toSummary(Risk r) {
        int inherent = RiskScoring.inherent(r.getLikelihood(), r.getImpact());
        int residual = residualOf(r, inherent);
        return new RiskSummaryResponse(r.getId(), r.getTitle(), r.getCategory(), r.getOwner(),
                r.getLikelihood(), r.getImpact(), r.getStatus(),
                inherent, SeverityBand.of(inherent), residual, SeverityBand.of(residual),
                r.getMitigations().size(), r.getCreatedAt(), r.getUpdatedAt());
    }

    private int residualOf(Risk r, int inherent) {
        List<Integer> effectiveness = r.getMitigations().stream().map(Mitigation::getEffectiveness).toList();
        return calculator.residual(inherent, effectiveness);
    }
}
