package com.dheeraj.airline.hold.mapper;

import com.dheeraj.airline.hold.dto.SeatHoldResponse;
import com.dheeraj.airline.hold.entity.SeatHold;
import org.springframework.stereotype.Component;

@Component
public class HoldMapper {

    public SeatHoldResponse toResponse(SeatHold seatHold) {
        return SeatHoldResponse.builder()
                .id(seatHold.getId())
                .flightId(seatHold.getFlightId())
                .seatId(seatHold.getSeatId())
                .holdReference(seatHold.getHoldReference())
                .passengerName(seatHold.getPassengerName())
                .passengerEmail(seatHold.getPassengerEmail())
                .status(seatHold.getStatus())
                .heldAt(seatHold.getHeldAt())
                .expiresAt(seatHold.getExpiresAt())
                .createdAt(seatHold.getCreatedAt())
                .updatedAt(seatHold.getUpdatedAt())
                .build();
    }
}
