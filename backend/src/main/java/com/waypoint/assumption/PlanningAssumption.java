package com.waypoint.assumption;

import com.waypoint.household.Household;
import com.waypoint.household.SourceType;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A household-scoped planning belief, explicitly distinct from a confirmed
 * financial fact. Every field except {@link #supersededBy} is immutable once
 * persisted; a changed belief is recorded by creating a new version and
 * linking this one to it, never by editing this row in place.
 */
@Entity
@Table(name = "planning_assumptions")
public class PlanningAssumption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false, updatable = false)
    private Household household;

    @Column(nullable = false, updatable = false)
    private String name;

    @Column(nullable = false, updatable = false, length = 2000)
    private String value;

    @Column(name = "value_type", nullable = false, updatable = false, length = 100)
    private String valueType;

    @Column(updatable = false, length = 2000)
    private String notes;

    @Column(name = "effective_from", nullable = false, updatable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_until", updatable = false)
    private LocalDate effectiveUntil;

    @Column(name = "review_date", nullable = false, updatable = false)
    private LocalDate reviewDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 16, updatable = false)
    private SourceType sourceType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "superseded_by_id")
    private PlanningAssumption supersededBy;

    protected PlanningAssumption() {
    }

    public PlanningAssumption(
            Household household,
            String name,
            String value,
            String valueType,
            String notes,
            LocalDate effectiveFrom,
            LocalDate effectiveUntil,
            LocalDate reviewDate
    ) {
        this.household = household;
        this.name = name;
        this.value = value;
        this.valueType = valueType;
        this.notes = notes;
        this.effectiveFrom = effectiveFrom;
        this.effectiveUntil = effectiveUntil;
        this.reviewDate = reviewDate;
        this.sourceType = SourceType.MANUAL_ENTRY;
    }

    /**
     * Active means not yet superseded and temporally applicable on the given
     * date. The caller always supplies {@code asOf} explicitly; this method
     * never consults the system clock.
     */
    public boolean isActiveAsOf(LocalDate asOf) {
        if (supersededBy != null) {
            return false;
        }
        if (asOf.isBefore(effectiveFrom)) {
            return false;
        }
        return effectiveUntil == null || !asOf.isAfter(effectiveUntil);
    }

    /**
     * Links this version to the replacement that superseded it. Package-
     * private: only {@link PlanningAssumptionService} may call this, and
     * only once per record, as part of atomically creating a replacement.
     */
    void linkSupersededBy(PlanningAssumption replacement) {
        if (this.supersededBy != null) {
            throw new IllegalStateException("Planning assumption is already superseded: " + this.id);
        }
        this.supersededBy = replacement;
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

    public String getValue() {
        return value;
    }

    public String getValueType() {
        return valueType;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDate getEffectiveUntil() {
        return effectiveUntil;
    }

    public LocalDate getReviewDate() {
        return reviewDate;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public PlanningAssumption getSupersededBy() {
        return supersededBy;
    }
}
