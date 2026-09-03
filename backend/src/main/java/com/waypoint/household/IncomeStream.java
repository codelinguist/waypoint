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
@Table(name = "income_streams")
public class IncomeStream {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false, updatable = false)
    private Household household;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "income_type", nullable = false, length = 32)
    private IncomeType incomeType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Frequency frequency;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "compensation_classification", nullable = false, length = 16)
    private CompensationClassification compensationClassification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private IncomeCertainty certainty;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 16)
    private SourceType sourceType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IncomeStream() {
    }

    public IncomeStream(
            Household household,
            String name,
            IncomeType incomeType,
            BigDecimal amount,
            Frequency frequency,
            String currency,
            CompensationClassification compensationClassification,
            IncomeCertainty certainty,
            LocalDate startDate,
            LocalDate endDate
    ) {
        this.household = household;
        this.name = name;
        this.incomeType = incomeType;
        this.amount = amount;
        this.frequency = frequency;
        this.currency = currency;
        this.compensationClassification = compensationClassification;
        this.certainty = certainty;
        this.startDate = startDate;
        this.endDate = endDate;
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

    public IncomeType getIncomeType() {
        return incomeType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public String getCurrency() {
        return currency;
    }

    public CompensationClassification getCompensationClassification() {
        return compensationClassification;
    }

    public IncomeCertainty getCertainty() {
        return certainty;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
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
