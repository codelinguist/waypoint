package com.waypoint.household;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "assets")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false, updatable = false)
    private Household household;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 32)
    private AssetType assetType;

    @Column(name = "estimated_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal estimatedValue;

    @Column(name = "planning_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal planningValue;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "valued_at", nullable = false)
    private LocalDate valuedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Liquidity liquidity;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 16)
    private SourceType sourceType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Asset() {
    }

    public Asset(
            Household household,
            String name,
            AssetType assetType,
            BigDecimal estimatedValue,
            BigDecimal planningValue,
            String currency,
            LocalDate valuedAt,
            Liquidity liquidity
    ) {
        this.household = household;
        this.name = name;
        this.assetType = assetType;
        this.estimatedValue = estimatedValue;
        this.planningValue = planningValue;
        this.currency = currency;
        this.valuedAt = valuedAt;
        this.liquidity = liquidity;
        this.sourceType = SourceType.MANUAL_ENTRY;
    }

    public UUID getId() {
        return id;
    }

    public Household getHousehold() {
        return household;
    }

    public String getName() {
        return name;
    }

    public AssetType getAssetType() {
        return assetType;
    }

    public BigDecimal getEstimatedValue() {
        return estimatedValue;
    }

    public BigDecimal getPlanningValue() {
        return planningValue;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getValuedAt() {
        return valuedAt;
    }

    public Liquidity getLiquidity() {
        return liquidity;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
