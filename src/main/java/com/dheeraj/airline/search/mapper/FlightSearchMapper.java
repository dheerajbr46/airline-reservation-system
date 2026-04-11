package com.dheeraj.airline.search.mapper;

import com.dheeraj.airline.flight.entity.Flight;
import com.dheeraj.airline.search.dto.FlightSearchResponse;
import org.springframework.stereotype.Component;

@Component
public class FlightSearchMapper {

    public FlightSearchResponse toResponse(Flight flight, long availableSeats) {
        return FlightSearchResponse.builder()
                .flightId(flight.getId())
                .flightNumber(flight.getFlightNumber())
                .aircraftCode(flight.getAircraftCode())
                .origin(flight.getOrigin())
                .destination(flight.getDestination())
                .departureTime(flight.getDepartureTime())
                .arrivalTime(flight.getArrivalTime())
                .status(flight.getStatus())
                .availableSeats(availableSeats)
                .build();
    }
}
