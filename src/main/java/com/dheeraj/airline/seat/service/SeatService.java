package com.dheeraj.airline.seat.service;

import com.dheeraj.airline.seat.dto.CreateSeatRequest;
import com.dheeraj.airline.seat.dto.SeatResponse;
import java.util.List;
import java.util.UUID;

public interface SeatService {

    SeatResponse createSeat(CreateSeatRequest request);

    List<SeatResponse> getSeatsByFlight(UUID flightId);

    List<SeatResponse> getAvailableSeatsByFlight(UUID flightId);
}
