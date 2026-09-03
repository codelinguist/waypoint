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
@Table(name = "liabilities")
public class Liability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false, updatable = false)
    private Household household;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "liability_type", nullable = false, length = 32)
    private LiabilityType liabilityType;

    @Column(name = "outstanding_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingBalance;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "balance_as_of", nullable = false)
    private LocalDate balanceAsOf;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 16)
    private SourceType sourceType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Liability() {
    }

    public Liability(
            Household household,
            String name,
            LiabilityType liabilityType,
            BigDecimal outstandingBalance,
            String currency,
            LocalDate balanceAsOf
    ) {
        this.household = household;
        this.name = name;
        this.liabilityType = liabilityType;
        this.outstandingBalance = outstandingBalance;
        this.currency = currency;
        this.balanceAsOf = balanceAsOf;
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

    public LiabilityType getLiabilityType() {
        return liabilityType;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public String getCurrency() {
        return currency;
    }

    public LocalDate getBalanceAsOf() {
        return balanceAsOf;
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
