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
 * A household-scoped planning belief, never a confirmed financial fact.
 * Records are immutable; a change in belief is captured by superseding this
 * version with a new one rather than editing it in place.
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

    @Column(nullable = false, updatable = false)
    private String value;

    @Column(name = "value_type", nullable = false, updatable = false)
    private String valueType;

    @Column(updatable = false)
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "superseded_by_id")
    private PlanningAssumption supersededBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

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

    public UUID getSupersededById() {
        return supersededBy == null ? null : supersededBy.getId();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
