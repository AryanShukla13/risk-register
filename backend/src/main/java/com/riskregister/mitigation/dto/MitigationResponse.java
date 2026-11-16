package com.riskregister.mitigation.dto;

import java.time.Instant;

public record MitigationResponse(Long id, Long riskId, String description, int effectiveness, Instant createdAt) {}
