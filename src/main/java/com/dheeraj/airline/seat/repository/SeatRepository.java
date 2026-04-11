package com.dheeraj.airline.seat.repository;

import com.dheeraj.airline.seat.entity.CabinClass;
import com.dheeraj.airline.seat.entity.Seat;
import com.dheeraj.airline.seat.entity.SeatStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

    List<Seat> findByFlight_IdOrderBySeatNumberAsc(UUID flightId);

    List<Seat> findByFlight_IdAndStatusOrderBySeatNumberAsc(UUID flightId, SeatStatus status);

    long countByFlight_IdAndStatus(UUID flightId, SeatStatus status);

    Optional<Seat> findByFlight_IdAndSeatNumber(UUID flightId, String seatNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.id = :seatId")
    Optional<Seat> findByIdForUpdate(@Param("seatId") UUID seatId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select s
        from Seat s
        where s.flight.id = :flightId
          and s.seatNumber = :seatNumber
        """)
    Optional<Seat> findByFlightIdAndSeatNumberForUpdate(
            @Param("flightId") UUID flightId,
            @Param("seatNumber") String seatNumber
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select s
        from Seat s
        where s.flight.id = :flightId
          and s.status = com.dheeraj.airline.seat.entity.SeatStatus.AVAILABLE
        order by s.seatNumber asc
        limit 1
        """)
    Optional<Seat> findFirstAvailableSeatByFlightIdForUpdate(@Param("flightId") UUID flightId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select s
        from Seat s
        where s.flight.id = :flightId
          and s.cabinClass = :cabinClass
          and s.status = com.dheeraj.airline.seat.entity.SeatStatus.AVAILABLE
        order by s.seatNumber asc
        limit 1
        """)
    Optional<Seat> findFirstAvailableSeatByFlightIdAndCabinClassForUpdate(
            @Param("flightId") UUID flightId,
            @Param("cabinClass") CabinClass cabinClass
    );
}
