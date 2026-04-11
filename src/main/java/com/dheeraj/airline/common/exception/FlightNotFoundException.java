package com.dheeraj.airline.common.exception;

import java.util.UUID;

public class FlightNotFoundException extends ResourceNotFoundException {

    public FlightNotFoundException(UUID flightId) {
        super("Flight not found with id: " + flightId);
    }
}
