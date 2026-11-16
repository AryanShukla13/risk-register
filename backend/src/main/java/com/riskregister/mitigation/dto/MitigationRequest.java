package com.riskregister.mitigation.dto;

import jakarta.validation.constraints.*;

public record MitigationRequest(
        @NotBlank @Size(max = 1000) String description,
        @NotNull @Min(value = 1, message = "must be an integer between 1 and 5")
        @Max(value = 5, message = "must be an integer between 1 and 5") Integer effectiveness) {}
