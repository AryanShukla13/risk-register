package com.riskregister.risk.dto;

import com.riskregister.mitigation.dto.MitigationResponse;
import com.riskregister.risk.RiskCategory;
import com.riskregister.risk.RiskStatus;
import com.riskregister.scoring.SeverityBand;
import java.time.Instant;
import java.util.List;

public record RiskResponse(
        Long id, String title, String description, RiskCategory category, String owner,
        int likelihood, int impact, RiskStatus status,
        int inherentScore, SeverityBand inherentSeverity,
        int residualScore, SeverityBand residualSeverity,
        List<MitigationResponse> mitigations, Instant createdAt, Instant updatedAt) {}
