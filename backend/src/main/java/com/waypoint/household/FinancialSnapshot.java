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
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

/**
 * An immutable, point-in-time capture of a household's known balance-sheet
 * state. There is no update path: {@code asOfDate} is the caller-requested
 * calendar date used to select eligible source records, while
 * {@code capturedAt} is the actual generation time, so the two are never
 * conflated.
 */
@Entity
@Table(name = "financial_snapshots")
public class FinancialSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false, updatable = false)
    private Household household;

    @Column(name = "as_of_date", nullable = false)
    private LocalDate asOfDate;

    @CreationTimestamp
    @Column(name = "captured_at", nullable = false, updatable = false)
    private Instant capturedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 16)
    private SourceType sourceType;

    protected FinancialSnapshot() {
    }

    public FinancialSnapshot(Household household, LocalDate asOfDate) {
        this.household = household;
        this.asOfDate = asOfDate;
        this.sourceType = SourceType.MANUAL_ENTRY;
    }

    public UUID getId() {
        return id;
    }

    public Household getHousehold() {
        return household;
    }

    public LocalDate getAsOfDate() {
        return asOfDate;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public SourceType getSourceType() {
        return sourceType;
    }
}
