package com.waypoint.household;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LiabilityService {

    private final HouseholdRepository householdRepository;
    private final LiabilityRepository liabilityRepository;
    private final LiabilityBalanceHistoryRepository liabilityBalanceHistoryRepository;

    public LiabilityService(
            HouseholdRepository householdRepository,
            LiabilityRepository liabilityRepository,
            LiabilityBalanceHistoryRepository liabilityBalanceHistoryRepository
    ) {
        this.householdRepository = householdRepository;
        this.liabilityRepository = liabilityRepository;
        this.liabilityBalanceHistoryRepository = liabilityBalanceHistoryRepository;
    }

    public Liability createLiability(
            UUID householdId,
            String name,
            LiabilityType liabilityType,
            BigDecimal outstandingBalance,
            String currency,
            LocalDate balanceAsOf
    ) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException(householdId));
        Liability liability = new Liability(
                household,
                name.trim(),
                liabilityType,
                outstandingBalance,
                currency.trim().toUpperCase(),
                balanceAsOf
        );
        return liabilityRepository.save(liability);
    }

    @Transactional(readOnly = true)
    public Liability getLiability(UUID householdId, UUID liabilityId) {
        if (!householdRepository.existsById(householdId)) {
            throw new HouseholdNotFoundException(householdId);
        }
        return liabilityRepository.findByIdAndHousehold_Id(liabilityId, householdId)
                .orElseThrow(() -> new LiabilityNotFoundException(liabilityId));
    }

    @Transactional(readOnly = true)
    public List<Liability> listLiabilities(UUID householdId) {
        if (!householdRepository.existsById(householdId)) {
            throw new HouseholdNotFoundException(householdId);
        }
        return liabilityRepository.findByHousehold_IdOrderByCreatedAtAscIdAsc(householdId);
    }

    /**
     * Replaces a liability's current outstanding balance and appends an
     * immutable before/after audit row, atomically. {@code expectedRevision}
     * guards against lost updates: {@link LiabilityRepository#applyBalance}
     * conditions its single update statement on the row's current revision
     * still matching, so a caller-stale revision and a genuinely concurrent
     * conflicting write both surface identically as zero rows updated — with
     * exactly one audit row, since a rejected attempt never reaches the
     * append below.
     */
    public LiabilityBalanceHistory recordBalance(
            UUID householdId,
            UUID liabilityId,
            BigDecimal outstandingBalance,
            LocalDate balanceAsOf,
            String reason,
            long expectedRevision
    ) {
        if (!householdRepository.existsById(householdId)) {
            throw new HouseholdNotFoundException(householdId);
        }
        Liability liability = liabilityRepository.findByIdAndHousehold_Id(liabilityId, householdId)
                .orElseThrow(() -> new LiabilityNotFoundException(liabilityId));

        BigDecimal previousBalance = liability.getOutstandingBalance();
        LocalDate previousBalanceAsOf = liability.getBalanceAsOf();
        SourceType previousSourceType = liability.getSourceType();

        int updatedRows = liabilityRepository.applyBalance(
                liabilityId, householdId, outstandingBalance, balanceAsOf, Instant.now(), expectedRevision);
        if (updatedRows == 0) {
            throw new StaleLiabilityRevisionException(liabilityId, expectedRevision);
        }

        Liability updated = liabilityRepository.findByIdAndHousehold_Id(liabilityId, householdId)
                .orElseThrow(() -> new LiabilityNotFoundException(liabilityId));

        LiabilityBalanceHistory history = new LiabilityBalanceHistory(
                updated,
                updated.getCurrency(),
                previousBalance,
                previousBalanceAsOf,
                previousSourceType,
                outstandingBalance,
                balanceAsOf,
                updated.getSourceType(),
                reason.trim(),
                updated.getRevision()
        );
        return liabilityBalanceHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<LiabilityBalanceHistory> listBalanceHistory(UUID householdId, UUID liabilityId) {
        if (!householdRepository.existsById(householdId)) {
            throw new HouseholdNotFoundException(householdId);
        }
        if (!liabilityRepository.existsByIdAndHousehold_Id(liabilityId, householdId)) {
            throw new LiabilityNotFoundException(liabilityId);
        }
        return liabilityBalanceHistoryRepository.findByLiability_IdOrderByRevisionAsc(liabilityId);
    }
}
