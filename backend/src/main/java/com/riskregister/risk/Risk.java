package com.riskregister.risk;

import com.riskregister.mitigation.Mitigation;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "risks")
public class Risk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskCategory category;

    @Column(nullable = false, length = 100)
    private String owner;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(nullable = false)
    private int likelihood;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(nullable = false)
    private int impact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "risk", cascade = CascadeType.REMOVE)
    private List<Mitigation> mitigations = new ArrayList<>();

    protected Risk() {}

    public Risk(String title, String description, RiskCategory category, String owner,
                int likelihood, int impact, RiskStatus status) {
        apply(title, description, category, owner, likelihood, impact, status);
    }

    public void apply(String title, String description, RiskCategory category, String owner,
                      int likelihood, int impact, RiskStatus status) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.owner = owner;
        this.likelihood = likelihood;
        this.impact = impact;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public RiskCategory getCategory() { return category; }
    public String getOwner() { return owner; }
    public int getLikelihood() { return likelihood; }
    public int getImpact() { return impact; }
    public RiskStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<Mitigation> getMitigations() { return mitigations; }
}
