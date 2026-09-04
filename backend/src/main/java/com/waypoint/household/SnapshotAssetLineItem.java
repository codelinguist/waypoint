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
 * A copied, immutable observation of one {@link Asset} at snapshot capture
 * time. {@code sourceAssetId} is retained UUID metadata, not a live foreign
 * key, so a later change to (or eventual removal of) the source asset cannot
 * alter this historical record.
 */
@Entity
@Table(name = "snapshot_asset_line_items")
public class SnapshotAssetLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_id", nullable = false, updatable = false)
    private FinancialSnapshot snapshot;

    @Column(name = "source_asset_id", nullable = false, updatable = false)
    private UUID sourceAssetId;

    @Column(nullable = false, updatable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 32, updatable = false)
    private AssetType assetType;

    @Column(nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "source_date", nullable = false, updatable = false)
    private LocalDate sourceDate;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal value;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SnapshotAssetLineItem() {
    }

    public SnapshotAssetLineItem(
            FinancialSnapshot snapshot,
            UUID sourceAssetId,
            String name,
            AssetType assetType,
            String currency,
            LocalDate sourceDate,
            BigDecimal value
    ) {
        this.snapshot = snapshot;
        this.sourceAssetId = sourceAssetId;
        this.name = name;
        this.assetType = assetType;
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

    public UUID getSourceAssetId() {
        return sourceAssetId;
    }

    public String getName() {
        return name;
    }

    public AssetType getAssetType() {
        return assetType;
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
