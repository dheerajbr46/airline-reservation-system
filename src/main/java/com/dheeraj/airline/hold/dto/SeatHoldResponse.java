package com.dheeraj.airline.hold.dto;

import com.dheeraj.airline.hold.entity.HoldStatus;
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
public class SeatHoldResponse {

    private UUID id;
    private UUID flightId;
    private UUID seatId;
    private String holdReference;
    private String passengerName;
    private String passengerEmail;
    private HoldStatus status;
    private LocalDateTime heldAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
