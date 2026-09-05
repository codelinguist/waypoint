package com.waypoint.household;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FinancialGoalService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int PROGRESS_SCALE = 2;

    private final HouseholdRepository householdRepository;
    private final FinancialGoalRepository financialGoalRepository;

    public FinancialGoalService(
            HouseholdRepository householdRepository,
            FinancialGoalRepository financialGoalRepository
    ) {
        this.householdRepository = householdRepository;
        this.financialGoalRepository = financialGoalRepository;
    }

    public FinancialGoal createGoal(
            UUID householdId,
            String name,
            BigDecimal targetAmount,
            String currency,
            LocalDate targetDate,
            Integer priority,
            BigDecimal currentAmount
    ) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException(householdId));
        FinancialGoal goal = new FinancialGoal(
                household,
                name.trim(),
                targetAmount,
                currency.trim().toUpperCase(),
                targetDate,
                priority,
                currentAmount
        );
        return financialGoalRepository.save(goal);
    }

    @Transactional(readOnly = true)
    public FinancialGoal getGoal(UUID householdId, UUID goalId) {
        if (!householdRepository.existsById(householdId)) {
            throw new HouseholdNotFoundException(householdId);
        }
        return financialGoalRepository.findByIdAndHousehold_Id(goalId, householdId)
                .orElseThrow(() -> new FinancialGoalNotFoundException(goalId));
    }

    @Transactional(readOnly = true)
    public List<FinancialGoal> listGoals(UUID householdId) {
        if (!householdRepository.existsById(householdId)) {
            throw new HouseholdNotFoundException(householdId);
        }
        return financialGoalRepository.findByHousehold_IdOrderByPriorityAscCreatedAtAscIdAsc(householdId);
    }

    public static BigDecimal remainingAmount(FinancialGoal goal) {
        return goal.getTargetAmount().subtract(goal.getCurrentAmount());
    }

    public static BigDecimal progressPercentage(FinancialGoal goal) {
        BigDecimal rawPercentage = goal.getCurrentAmount()
                .divide(goal.getTargetAmount(), PROGRESS_SCALE + 2, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED)
                .setScale(PROGRESS_SCALE, RoundingMode.HALF_UP);
        if (rawPercentage.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(PROGRESS_SCALE);
        }
        if (rawPercentage.compareTo(ONE_HUNDRED) > 0) {
            return ONE_HUNDRED.setScale(PROGRESS_SCALE);
        }
        return rawPercentage;
    }
}
