package com.dheeraj.airline.waitlist.repository;

import com.dheeraj.airline.seat.entity.CabinClass;
import com.dheeraj.airline.waitlist.entity.WaitlistEntry;
import com.dheeraj.airline.waitlist.entity.WaitlistStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, UUID> {

    List<WaitlistEntry> findByFlightIdAndStatusOrderByQueuePositionAsc(
            UUID flightId,
            WaitlistStatus status
    );

    List<WaitlistEntry> findByFlightIdAndPreferredCabinClassAndStatusOrderByQueuePositionAsc(
            UUID flightId,
            CabinClass preferredCabinClass,
            WaitlistStatus status
    );

    Optional<WaitlistEntry> findById(UUID waitlistEntryId);

    @Query("""
        select coalesce(max(w.queuePosition), 0)
        from WaitlistEntry w
        where w.flightId = :flightId
        """)
    int findMaxQueuePositionByFlightId(@Param("flightId") UUID flightId);

    boolean existsByFlightIdAndPassengerEmailIgnoreCaseAndStatus(
            UUID flightId,
            String passengerEmail,
            WaitlistStatus status
    );
}
