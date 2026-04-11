package com.dheeraj.airline.flight.mapper;

import com.dheeraj.airline.flight.dto.CreateFlightRequest;
import com.dheeraj.airline.flight.dto.FlightResponse;
import com.dheeraj.airline.flight.entity.Flight;
import org.springframework.stereotype.Component;

@Component
public class FlightMapper {

    public Flight toEntity(CreateFlightRequest request) {
        Flight flight = new Flight();
        flight.setFlightNumber(request.getFlightNumber());
        flight.setAircraftCode(request.getAircraftCode());
        flight.setOrigin(request.getOrigin());
        flight.setDestination(request.getDestination());
        flight.setDepartureTime(request.getDepartureTime());
        flight.setArrivalTime(request.getArrivalTime());
        return flight;
    }

    public FlightResponse toResponse(Flight flight) {
        return FlightResponse.builder()
                .flightId(flight.getId())
                .flightNumber(flight.getFlightNumber())
                .aircraftCode(flight.getAircraftCode())
                .origin(flight.getOrigin())
                .destination(flight.getDestination())
                .departureTime(flight.getDepartureTime())
                .arrivalTime(flight.getArrivalTime())
                .status(flight.getStatus())
                .createdAt(flight.getCreatedAt())
                .updatedAt(flight.getUpdatedAt())
                .build();
    }
}
