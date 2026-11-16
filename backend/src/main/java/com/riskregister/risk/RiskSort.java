package com.riskregister.risk;

import com.riskregister.common.BadRequestException;

/** Supported list orderings. Wire format: {@code residualScore,desc}. */
public enum RiskSort {
    CREATED_DESC("createdAt,desc"),
    RESIDUAL_DESC("residualScore,desc"),
    RESIDUAL_ASC("residualScore,asc");

    private final String param;

    RiskSort(String param) {
        this.param = param;
    }

    public static RiskSort parse(String value) {
        if (value == null || value.isBlank()) {
            return CREATED_DESC;
        }
        for (RiskSort s : values()) {
            if (s.param.equalsIgnoreCase(value.trim())) {
                return s;
            }
        }
        throw new BadRequestException("sort", "must be one of createdAt,desc | residualScore,desc | residualScore,asc");
    }
}
