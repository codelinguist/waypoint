package com.waypoint.planning.multigoalfunding;

import com.waypoint.planning.goalcontribution.GoalContributionResult;

/**
 * One goal's independently calculated contribution requirement, tagged with
 * the caller-supplied {@code reference} so results can be matched back to
 * their input regardless of the order goals are processed internally.
 */
public record GoalFundingCheckItemResult(
        String reference,
        GoalContributionResult contribution
) {
}
