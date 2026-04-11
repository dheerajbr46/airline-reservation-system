package com.dheeraj.airline.flight.repository;

import com.dheeraj.airline.flight.entity.Flight;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FlightRepository extends JpaRepository<Flight, UUID> {

    Optional<Flight> findByFlightNumber(String flightNumber);

    @Query("""
        select f
        from Flight f
        where lower(f.origin) = lower(:origin)
          and lower(f.destination) = lower(:destination)
          and f.departureTime >= :startOfDay
          and f.departureTime < :endOfDay
        order by f.departureTime asc
        """)
    List<Flight> searchByRouteAndDepartureWindow(
            @Param("origin") String origin,
            @Param("destination") String destination,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    default List<Flight> searchByOriginDestinationAndDepartureDate(
            String origin,
            String destination,
            LocalDate departureDate
    ) {
        return searchByRouteAndDepartureWindow(
                origin,
                destination,
                departureDate.atStartOfDay(),
                departureDate.plusDays(1).atStartOfDay()
        );
    }
}
