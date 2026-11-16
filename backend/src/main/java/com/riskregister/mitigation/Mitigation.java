package com.riskregister.mitigation;

import com.riskregister.risk.Risk;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "mitigations")
public class Mitigation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "risk_id", nullable = false, updatable = false)
    private Risk risk;

    @Column(nullable = false, length = 1000)
    private String description;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(nullable = false)
    private int effectiveness;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Mitigation() {}

    public Mitigation(Risk risk, String description, int effectiveness) {
        this.risk = risk;
        this.description = description;
        this.effectiveness = effectiveness;
    }

    public void apply(String description, int effectiveness) {
        this.description = description;
        this.effectiveness = effectiveness;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Risk getRisk() { return risk; }
    public String getDescription() { return description; }
    public int getEffectiveness() { return effectiveness; }
    public Instant getCreatedAt() { return createdAt; }
}
