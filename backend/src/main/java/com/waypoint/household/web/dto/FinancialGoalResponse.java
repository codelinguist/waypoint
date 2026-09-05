package com.waypoint.household.web.dto;

import com.waypoint.household.FinancialGoal;
import com.waypoint.household.FinancialGoalService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FinancialGoalResponse(
        UUID id,
        UUID householdId,
        String name,
        BigDecimal targetAmount,
        String currency,
        LocalDate targetDate,
        Integer priority,
        BigDecimal currentAmount,
        BigDecimal remainingAmount,
        BigDecimal progressPercentage,
        Instant createdAt,
        Instant updatedAt
) {

    public static FinancialGoalResponse from(FinancialGoal goal) {
        return new FinancialGoalResponse(
                goal.getId(),
                goal.getHousehold().getId(),
                goal.getName(),
                goal.getTargetAmount(),
                goal.getCurrency(),
                goal.getTargetDate(),
                goal.getPriority(),
                goal.getCurrentAmount(),
                FinancialGoalService.remainingAmount(goal),
                FinancialGoalService.progressPercentage(goal),
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }
}
