package com.dheeraj.airline.waitlist.dto;

import com.dheeraj.airline.seat.entity.CabinClass;
import com.dheeraj.airline.waitlist.entity.WaitlistStatus;
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
public class WaitlistEntryResponse {

    private UUID waitlistEntryId;
    private UUID flightId;
    private String passengerName;
    private String passengerEmail;
    private CabinClass preferredCabinClass;
    private WaitlistStatus status;
    private Integer queuePosition;
    private UUID promotedHoldId;
    private LocalDateTime createdAt;
}
