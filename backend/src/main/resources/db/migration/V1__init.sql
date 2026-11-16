CREATE TABLE risks (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title       VARCHAR(200)  NOT NULL,
    description VARCHAR(2000),
    category    VARCHAR(20)   NOT NULL
                CHECK (category IN ('OPERATIONAL','FINANCIAL','COMPLIANCE','SECURITY','STRATEGIC')),
    owner       VARCHAR(100)  NOT NULL,
    likelihood  SMALLINT      NOT NULL CHECK (likelihood BETWEEN 1 AND 5),
    impact      SMALLINT      NOT NULL CHECK (impact BETWEEN 1 AND 5),
    status      VARCHAR(20)   NOT NULL CHECK (status IN ('OPEN','MITIGATING','CLOSED')),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_risks_category ON risks(category);
CREATE INDEX idx_risks_status   ON risks(status);

CREATE TABLE mitigations (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    risk_id       BIGINT        NOT NULL REFERENCES risks(id) ON DELETE CASCADE,
    description   VARCHAR(1000) NOT NULL,
    effectiveness SMALLINT      NOT NULL CHECK (effectiveness BETWEEN 1 AND 5),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_mitigations_risk_id ON mitigations(risk_id);
