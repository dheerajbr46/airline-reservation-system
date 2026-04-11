package com.dheeraj.airline.common.exception;

import java.util.UUID;

public class SeatAlreadyExistsException extends DuplicateResourceException {

    public SeatAlreadyExistsException(UUID flightId, String seatNumber) {
        super("Seat already exists for flight " + flightId + " with seat number: " + seatNumber);
    }
}
