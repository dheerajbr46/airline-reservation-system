package com.dheeraj.airline.flight.service;

import com.dheeraj.airline.flight.dto.CreateFlightRequest;
import com.dheeraj.airline.flight.dto.FlightResponse;
import java.util.UUID;

public interface FlightService {

    FlightResponse createFlight(CreateFlightRequest request);

    FlightResponse getFlightById(UUID flightId);
}
