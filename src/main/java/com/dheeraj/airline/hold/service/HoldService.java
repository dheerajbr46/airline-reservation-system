package com.dheeraj.airline.hold.service;

import com.dheeraj.airline.common.response.PagedResponse;
import com.dheeraj.airline.hold.dto.HoldSeatRequest;
import com.dheeraj.airline.hold.dto.SeatHoldResponse;
import com.dheeraj.airline.hold.entity.HoldStatus;
import java.util.List;
import java.util.UUID;

public interface HoldService {

    SeatHoldResponse placeHold(HoldSeatRequest request);

    SeatHoldResponse releaseHold(UUID holdId);

    PagedResponse<SeatHoldResponse> getHoldHistory(
            String passengerEmail,
            UUID flightId,
            HoldStatus status,
            int page,
            int size,
            String sortBy,
            String direction
    );

    List<SeatHoldResponse> releaseExpiredHolds();
}
