package com.waypoint.household;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LiabilityBalanceHistoryRepository extends JpaRepository<LiabilityBalanceHistory, UUID> {

    @Query("SELECT h FROM LiabilityBalanceHistory h "
            + "JOIN FETCH h.liability l "
            + "JOIN FETCH l.household "
            + "WHERE l.id = :liabilityId "
            + "ORDER BY h.revision ASC")
    List<LiabilityBalanceHistory> findByLiability_IdOrderByRevisionAsc(@Param("liabilityId") UUID liabilityId);
}
