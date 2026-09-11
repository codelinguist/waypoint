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
 * An immutable, append-only record of one accepted balance replacement on a
 * {@link Liability}. Rows are never edited or deleted; {@code revision}
 * matches the liability's resulting {@code @Version} value and is unique per
 * liability, giving a deterministic, gap-free change order.
 */
@Entity
@Table(name = "liability_balance_history")
public class LiabilityBalanceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "liability_id", nullable = false, updatable = false)
    private Liability liability;

    @Column(nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "previous_balance", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal previousBalance;

    @Column(name = "previous_balance_as_of", nullable = false, updatable = false)
    private LocalDate previousBalanceAsOf;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_source_type", nullable = false, length = 16, updatable = false)
    private SourceType previousSourceType;

    @Column(name = "new_balance", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal newBalance;

    @Column(name = "new_balance_as_of", nullable = false, updatable = false)
    private LocalDate newBalanceAsOf;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_source_type", nullable = false, length = 16, updatable = false)
    private SourceType newSourceType;

    @Column(nullable = false, length = 500, updatable = false)
    private String reason;

    @Column(nullable = false, updatable = false)
    private long revision;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    protected LiabilityBalanceHistory() {
    }

    LiabilityBalanceHistory(
            Liability liability,
            String currency,
            BigDecimal previousBalance,
            LocalDate previousBalanceAsOf,
            SourceType previousSourceType,
            BigDecimal newBalance,
            LocalDate newBalanceAsOf,
            SourceType newSourceType,
            String reason,
            long revision
    ) {
        this.liability = liability;
        this.currency = currency;
        this.previousBalance = previousBalance;
        this.previousBalanceAsOf = previousBalanceAsOf;
        this.previousSourceType = previousSourceType;
        this.newBalance = newBalance;
        this.newBalanceAsOf = newBalanceAsOf;
        this.newSourceType = newSourceType;
        this.reason = reason;
        this.revision = revision;
    }

    public UUID getId() {
        return id;
    }

    public Liability getLiability() {
        return liability;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getPreviousBalance() {
        return previousBalance;
    }

    public LocalDate getPreviousBalanceAsOf() {
        return previousBalanceAsOf;
    }

    public SourceType getPreviousSourceType() {
        return previousSourceType;
    }

    public BigDecimal getNewBalance() {
        return newBalance;
    }

    public LocalDate getNewBalanceAsOf() {
        return newBalanceAsOf;
    }

    public SourceType getNewSourceType() {
        return newSourceType;
    }

    public String getReason() {
        return reason;
    }

    public long getRevision() {
        return revision;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
