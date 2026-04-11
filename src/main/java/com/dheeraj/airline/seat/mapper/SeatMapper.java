package com.dheeraj.airline.seat.mapper;

import com.dheeraj.airline.flight.entity.Flight;
import com.dheeraj.airline.seat.dto.CreateSeatRequest;
import com.dheeraj.airline.seat.dto.SeatResponse;
import com.dheeraj.airline.seat.entity.Seat;
import org.springframework.stereotype.Component;

@Component
public class SeatMapper {

    public Seat toEntity(CreateSeatRequest request, Flight flight) {
        Seat seat = new Seat();
        seat.setFlight(flight);
        seat.setSeatNumber(request.getSeatNumber());
        seat.setCabinClass(request.getCabinClass());
        return seat;
    }

    public SeatResponse toResponse(Seat seat) {
        return SeatResponse.builder()
                .seatId(seat.getId())
                .flightId(seat.getFlight().getId())
                .seatNumber(seat.getSeatNumber())
                .cabinClass(seat.getCabinClass())
                .status(seat.getStatus())
                .createdAt(seat.getCreatedAt())
                .updatedAt(seat.getUpdatedAt())
                .build();
    }
}
