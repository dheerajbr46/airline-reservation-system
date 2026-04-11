package com.dheeraj.airline.common.exception;

public class FlightAlreadyExistsException extends DuplicateResourceException {

    public FlightAlreadyExistsException(String flightNumber) {
        super("Flight already exists with flight number: " + flightNumber);
    }
}
