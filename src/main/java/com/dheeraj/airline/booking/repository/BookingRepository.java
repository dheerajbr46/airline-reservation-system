package com.dheeraj.airline.booking.repository;

import com.dheeraj.airline.booking.entity.Booking;
import com.dheeraj.airline.booking.entity.BookingStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    Optional<Booking> findByBookingReference(String bookingReference);

    List<Booking> findByFlightId(UUID flightId);

    List<Booking> findBySeatId(UUID seatId);

    Optional<Booking> findByHoldId(UUID holdId);

    @Query(
            value = """
                select b
                from Booking b
                join SeatHold sh on sh.id = b.holdId
                where (:passengerEmail is null or lower(sh.passengerEmail) = lower(:passengerEmail))
                  and (:flightId is null or b.flightId = :flightId)
                  and (:status is null or b.status = :status)
                """,
            countQuery = """
                select count(b)
                from Booking b
                join SeatHold sh on sh.id = b.holdId
                where (:passengerEmail is null or lower(sh.passengerEmail) = lower(:passengerEmail))
                  and (:flightId is null or b.flightId = :flightId)
                  and (:status is null or b.status = :status)
                """
    )
    Page<Booking> findBookingHistory(
            @Param("passengerEmail") String passengerEmail,
            @Param("flightId") UUID flightId,
            @Param("status") BookingStatus status,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :bookingId")
    Optional<Booking> findByIdForUpdate(@Param("bookingId") UUID bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select b
        from Booking b
        where b.bookingReference = :bookingReference
        """)
    Optional<Booking> findByBookingReferenceForUpdate(@Param("bookingReference") String bookingReference);
}
