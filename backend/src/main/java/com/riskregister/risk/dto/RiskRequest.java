package com.riskregister.risk.dto;

import com.riskregister.risk.RiskCategory;
import com.riskregister.risk.RiskStatus;
import jakarta.validation.constraints.*;

public record RiskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotNull RiskCategory category,
        @NotBlank @Size(max = 100) String owner,
        @NotNull @Min(value = 1, message = "must be an integer between 1 and 5")
        @Max(value = 5, message = "must be an integer between 1 and 5") Integer likelihood,
        @NotNull @Min(value = 1, message = "must be an integer between 1 and 5")
        @Max(value = 5, message = "must be an integer between 1 and 5") Integer impact,
        @NotNull RiskStatus status) {}
