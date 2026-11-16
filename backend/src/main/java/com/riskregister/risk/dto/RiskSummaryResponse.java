package com.riskregister.risk.dto;

import com.riskregister.risk.RiskCategory;
import com.riskregister.risk.RiskStatus;
import com.riskregister.scoring.SeverityBand;
import java.time.Instant;

public record RiskSummaryResponse(
        Long id, String title, RiskCategory category, String owner,
        int likelihood, int impact, RiskStatus status,
        int inherentScore, SeverityBand inherentSeverity,
        int residualScore, SeverityBand residualSeverity,
        int mitigationCount, Instant createdAt, Instant updatedAt) {}
