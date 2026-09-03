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
 * A copied, immutable observation of one {@link Liability} at snapshot
 * capture time. {@code sourceLiabilityId} is retained UUID metadata, not a
 * live foreign key, so a later change to (or eventual removal of) the source
 * liability cannot alter this historical record.
 */
@Entity
@Table(name = "snapshot_liability_line_items")
public class SnapshotLiabilityLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_id", nullable = false, updatable = false)
    private FinancialSnapshot snapshot;

    @Column(name = "source_liability_id", nullable = false, updatable = false)
    private UUID sourceLiabilityId;

    @Column(nullable = false, updatable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "liability_type", nullable = false, length = 32, updatable = false)
    private LiabilityType liabilityType;

    @Column(nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "source_date", nullable = false, updatable = false)
    private LocalDate sourceDate;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal value;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SnapshotLiabilityLineItem() {
    }

    public SnapshotLiabilityLineItem(
            FinancialSnapshot snapshot,
            UUID sourceLiabilityId,
            String name,
            LiabilityType liabilityType,
            String currency,
            LocalDate sourceDate,
            BigDecimal value
    ) {
        this.snapshot = snapshot;
        this.sourceLiabilityId = sourceLiabilityId;
        this.name = name;
        this.liabilityType = liabilityType;
        this.currency = currency;
        this.sourceDate = sourceDate;
        this.value = value;
    }

    public UUID getId() {
        return id;
    }

    public FinancialSnapshot getSnapshot() {
        return snapshot;
    }

    public UUID getSourceLiabilityId() {
        return sourceLiabilityId;
    }

    public String getName() {
        return name;
    }

    public LiabilityType getLiabilityType() {
        return liabilityType;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getSourceDate() {
        return sourceDate;
    }

    public BigDecimal getValue() {
        return value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
