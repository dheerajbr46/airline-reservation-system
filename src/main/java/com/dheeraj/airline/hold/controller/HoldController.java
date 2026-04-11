package com.dheeraj.airline.hold.controller;

import com.dheeraj.airline.common.response.PagedResponse;
import com.dheeraj.airline.hold.dto.HoldSeatRequest;
import com.dheeraj.airline.hold.dto.SeatHoldResponse;
import com.dheeraj.airline.hold.entity.HoldStatus;
import com.dheeraj.airline.hold.service.HoldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/holds")
@Tag(name = "Seat Holds", description = "Temporary seat holds and hold lifecycle operations")
public class HoldController {

    private final HoldService holdService;

    public HoldController(HoldService holdService) {
        this.holdService = holdService;
    }

    @PostMapping
    @Operation(
            summary = "Place temporary seat hold",
            description = "Locks an AVAILABLE seat, creates an ACTIVE hold, and sets a 10-minute expiration window."
    )
    public ResponseEntity<SeatHoldResponse> placeHold(@Valid @RequestBody HoldSeatRequest request) {
        SeatHoldResponse response = holdService.placeHold(request);
        return ResponseEntity.created(URI.create("/api/v1/holds/" + response.getId())).body(response);
    }

    @GetMapping
    @Operation(
            summary = "List hold history",
            description = "Returns paginated hold history with optional filters for passenger email, flight, and hold status."
    )
    public ResponseEntity<PagedResponse<SeatHoldResponse>> getHoldHistory(
            @RequestParam(required = false) String passengerEmail,
            @RequestParam(required = false) UUID flightId,
            @RequestParam(required = false) HoldStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ResponseEntity.ok(holdService.getHoldHistory(
                passengerEmail,
                flightId,
                status,
                page,
                size,
                sortBy,
                direction
        ));
    }

    @PostMapping("/{holdId}/release")
    @Operation(
            summary = "Release active hold",
            description = "Releases a specific ACTIVE hold and restores its seat to AVAILABLE. Confirmed, expired, or already released holds cannot be manually released."
    )
    public ResponseEntity<SeatHoldResponse> releaseHold(@PathVariable UUID holdId) {
        return ResponseEntity.ok(holdService.releaseHold(holdId));
    }

    @PostMapping("/release-expired")
    @Operation(
            summary = "Release expired holds",
            description = "Finds ACTIVE holds past their expiration time, marks them EXPIRED, and restores their seats to AVAILABLE."
    )
    public ResponseEntity<List<SeatHoldResponse>> releaseExpiredHolds() {
        return ResponseEntity.ok(holdService.releaseExpiredHolds());
    }
}
