package com.dheeraj.airline.hold.repository;

import com.dheeraj.airline.hold.entity.HoldStatus;
import com.dheeraj.airline.hold.entity.SeatHold;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatHoldRepository extends JpaRepository<SeatHold, UUID> {

    @Query("""
        select sh
        from SeatHold sh
        where sh.seatId = :seatId
          and sh.status = :status
          and sh.expiresAt > :currentTime
        order by sh.expiresAt asc
        """)
    List<SeatHold> findBySeatIdAndStatusAndNotExpired(
            @Param("seatId") UUID seatId,
            @Param("status") HoldStatus status,
            @Param("currentTime") LocalDateTime currentTime
    );

    @Query("""
        select sh
        from SeatHold sh
        where sh.status = com.dheeraj.airline.hold.entity.HoldStatus.ACTIVE
          and sh.expiresAt <= :currentTime
        order by sh.expiresAt asc
        """)
    List<SeatHold> findExpiredHolds(@Param("currentTime") LocalDateTime currentTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select sh
        from SeatHold sh
        where sh.status = com.dheeraj.airline.hold.entity.HoldStatus.ACTIVE
          and sh.expiresAt <= :currentTime
        order by sh.expiresAt asc
        """)
    List<SeatHold> findExpiredHoldsForUpdate(@Param("currentTime") LocalDateTime currentTime);

    Optional<SeatHold> findByHoldReference(String holdReference);

    @Query(
            value = """
                select sh
                from SeatHold sh
                where (:passengerEmail is null or lower(sh.passengerEmail) = lower(:passengerEmail))
                  and (:flightId is null or sh.flightId = :flightId)
                  and (:status is null or sh.status = :status)
                """,
            countQuery = """
                select count(sh)
                from SeatHold sh
                where (:passengerEmail is null or lower(sh.passengerEmail) = lower(:passengerEmail))
                  and (:flightId is null or sh.flightId = :flightId)
                  and (:status is null or sh.status = :status)
                """
    )
    Page<SeatHold> findHoldHistory(
            @Param("passengerEmail") String passengerEmail,
            @Param("flightId") UUID flightId,
            @Param("status") HoldStatus status,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sh from SeatHold sh where sh.id = :holdId")
    Optional<SeatHold> findByIdForUpdate(@Param("holdId") UUID holdId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select sh
        from SeatHold sh
        where sh.seatId = :seatId
          and sh.status = com.dheeraj.airline.hold.entity.HoldStatus.ACTIVE
          and sh.expiresAt > :currentTime
        order by sh.expiresAt asc
        """)
    List<SeatHold> findActiveHoldsBySeatIdForUpdate(
            @Param("seatId") UUID seatId,
            @Param("currentTime") LocalDateTime currentTime
    );
}
