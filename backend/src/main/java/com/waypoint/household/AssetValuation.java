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

/**
 * An immutable before/after audit record of one accepted change to an
 * {@link Asset}'s recorded valuation. {@code revision} is the asset's
 * resulting {@link Asset#getRevision()} after the change, unique per asset
 * and monotonically ordered — history ordering and "latest wins" both use
 * this revision, never {@code newValuedAt}. There is no update or delete
 * path for this entity by design: it is append-only audit evidence.
 */
@Entity
@Table(name = "asset_valuations")
public class AssetValuation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false, updatable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false, updatable = false)
    private Household household;

    @Column(nullable = false, updatable = false)
    private long revision;

    @Column(name = "previous_estimated_value", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal previousEstimatedValue;

    @Column(name = "previous_planning_value", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal previousPlanningValue;

    @Column(name = "previous_valued_at", nullable = false, updatable = false)
    private LocalDate previousValuedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_source_type", nullable = false, length = 16, updatable = false)
    private SourceType previousSourceType;

    @Column(name = "new_estimated_value", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal newEstimatedValue;

    @Column(name = "new_planning_value", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal newPlanningValue;

    @Column(name = "new_valued_at", nullable = false, updatable = false)
    private LocalDate newValuedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_source_type", nullable = false, length = 16, updatable = false)
    private SourceType newSourceType;

    @Column(nullable = false, length = 500, updatable = false)
    private String reason;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    protected AssetValuation() {
    }

    public AssetValuation(
            Asset asset,
            Household household,
            long revision,
            BigDecimal previousEstimatedValue,
            BigDecimal previousPlanningValue,
            LocalDate previousValuedAt,
            SourceType previousSourceType,
            BigDecimal newEstimatedValue,
            BigDecimal newPlanningValue,
            LocalDate newValuedAt,
            SourceType newSourceType,
            String reason
    ) {
        this.asset = asset;
        this.household = household;
        this.revision = revision;
        this.previousEstimatedValue = previousEstimatedValue;
        this.previousPlanningValue = previousPlanningValue;
        this.previousValuedAt = previousValuedAt;
        this.previousSourceType = previousSourceType;
        this.newEstimatedValue = newEstimatedValue;
        this.newPlanningValue = newPlanningValue;
        this.newValuedAt = newValuedAt;
        this.newSourceType = newSourceType;
        this.reason = reason;
    }

    public UUID getId() {
        return id;
    }

    public Asset getAsset() {
        return asset;
    }

    public Household getHousehold() {
        return household;
    }

    public long getRevision() {
        return revision;
    }

    public BigDecimal getPreviousEstimatedValue() {
        return previousEstimatedValue;
    }

    public BigDecimal getPreviousPlanningValue() {
        return previousPlanningValue;
    }

    public LocalDate getPreviousValuedAt() {
        return previousValuedAt;
    }

    public SourceType getPreviousSourceType() {
        return previousSourceType;
    }

    public BigDecimal getNewEstimatedValue() {
        return newEstimatedValue;
    }

    public BigDecimal getNewPlanningValue() {
        return newPlanningValue;
    }

    public LocalDate getNewValuedAt() {
        return newValuedAt;
    }

    public SourceType getNewSourceType() {
        return newSourceType;
    }

    public String getReason() {
        return reason;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
