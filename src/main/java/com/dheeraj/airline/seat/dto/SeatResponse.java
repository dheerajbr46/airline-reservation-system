package com.dheeraj.airline.seat.dto;

import com.dheeraj.airline.seat.entity.CabinClass;
import com.dheeraj.airline.seat.entity.SeatStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {

    private UUID seatId;
    private UUID flightId;
    private String seatNumber;
    private CabinClass cabinClass;
    private SeatStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
