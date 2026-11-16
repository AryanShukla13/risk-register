package com.riskregister.mitigation;

import com.riskregister.mitigation.dto.MitigationResponse;

public final class MitigationMapper {

    private MitigationMapper() {}

    public static MitigationResponse toResponse(Mitigation m) {
        return new MitigationResponse(m.getId(), m.getRisk().getId(), m.getDescription(),
                m.getEffectiveness(), m.getCreatedAt());
    }
}
