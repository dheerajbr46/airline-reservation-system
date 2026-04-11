package com.dheeraj.airline.waitlist.dto;

import com.dheeraj.airline.hold.dto.SeatHoldResponse;
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
public class PromoteWaitlistResponse {

    private WaitlistEntryResponse waitlistEntry;
    private SeatHoldResponse seatHold;
}
