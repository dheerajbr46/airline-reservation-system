package com.dheeraj.airline.waitlist.service;

import com.dheeraj.airline.waitlist.dto.JoinWaitlistRequest;
import com.dheeraj.airline.waitlist.dto.PromoteWaitlistResponse;
import com.dheeraj.airline.waitlist.dto.WaitlistEntryResponse;
import java.util.List;
import java.util.UUID;

public interface WaitlistService {

    WaitlistEntryResponse joinWaitlist(JoinWaitlistRequest request);

    List<WaitlistEntryResponse> getWaitlistForFlight(UUID flightId);

    PromoteWaitlistResponse promoteNext(UUID flightId);

    WaitlistEntryResponse cancelWaitlistEntry(UUID waitlistEntryId);
}
