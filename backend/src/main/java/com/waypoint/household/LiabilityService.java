package com.waypoint.household;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
     * must match the liability's current revision: a stale value from a prior
     * read is rejected outright, and two concurrent submissions starting from
     * the same revision resolve to exactly one success (caught here as an
     * {@link ObjectOptimisticLockingFailureException} from the losing flush)
     * and one {@link StaleLiabilityRevisionException} — with exactly one
     * audit row, since the losing attempt never reaches the append below.
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
        if (liability.getRevision() != expectedRevision) {
            throw new StaleLiabilityRevisionException(liabilityId, expectedRevision);
        }

        BigDecimal previousBalance = liability.getOutstandingBalance();
        LocalDate previousBalanceAsOf = liability.getBalanceAsOf();
        SourceType previousSourceType = liability.getSourceType();

        liability.replaceBalance(outstandingBalance, balanceAsOf);
        try {
            liabilityRepository.saveAndFlush(liability);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new StaleLiabilityRevisionException(liabilityId, expectedRevision);
        }

        LiabilityBalanceHistory history = new LiabilityBalanceHistory(
                liability,
                liability.getCurrency(),
                previousBalance,
                previousBalanceAsOf,
                previousSourceType,
                outstandingBalance,
                balanceAsOf,
                liability.getSourceType(),
                reason.trim(),
                liability.getRevision()
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
