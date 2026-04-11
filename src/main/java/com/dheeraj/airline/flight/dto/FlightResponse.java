package com.dheeraj.airline.flight.dto;

import com.dheeraj.airline.flight.entity.FlightStatus;
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
public class FlightResponse {

    private UUID flightId;
    private String flightNumber;
    private String aircraftCode;
    private String origin;
    private String destination;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private FlightStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
