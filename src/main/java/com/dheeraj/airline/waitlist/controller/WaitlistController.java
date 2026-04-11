package com.dheeraj.airline.waitlist.controller;

import com.dheeraj.airline.waitlist.dto.JoinWaitlistRequest;
import com.dheeraj.airline.waitlist.dto.PromoteWaitlistResponse;
import com.dheeraj.airline.waitlist.dto.WaitlistEntryResponse;
import com.dheeraj.airline.waitlist.service.WaitlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Waitlist", description = "Waitlist queue management and promotion into temporary holds")
public class WaitlistController {

    private final WaitlistService waitlistService;

    public WaitlistController(WaitlistService waitlistService) {
        this.waitlistService = waitlistService;
    }

    @PostMapping("/waitlist")
    @Operation(
            summary = "Join waitlist",
            description = "Adds a passenger to a flight waitlist as ACTIVE and assigns the next queue position."
    )
    public ResponseEntity<WaitlistEntryResponse> joinWaitlist(@Valid @RequestBody JoinWaitlistRequest request) {
        WaitlistEntryResponse response = waitlistService.joinWaitlist(request);
        return ResponseEntity
                .created(URI.create("/api/v1/waitlist/" + response.getWaitlistEntryId()))
                .body(response);
    }

    @GetMapping("/flights/{flightId}/waitlist")
    @Operation(
            summary = "List active waitlist entries",
            description = "Returns ACTIVE waitlist entries for a flight ordered by queue position."
    )
    public ResponseEntity<List<WaitlistEntryResponse>> getWaitlistForFlight(@PathVariable UUID flightId) {
        return ResponseEntity.ok(waitlistService.getWaitlistForFlight(flightId));
    }

    @PostMapping("/flights/{flightId}/waitlist/promote-next")
    @Operation(
            summary = "Promote next waitlist entry",
            description = "Finds the next eligible ACTIVE waitlist entry, reserves an available matching seat as an ACTIVE hold, and marks the entry PROMOTED."
    )
    public ResponseEntity<PromoteWaitlistResponse> promoteNext(@PathVariable UUID flightId) {
        return ResponseEntity.ok(waitlistService.promoteNext(flightId));
    }

    @PostMapping("/waitlist/{waitlistEntryId}/cancel")
    @Operation(
            summary = "Cancel waitlist entry",
            description = "Cancels an ACTIVE waitlist entry. PROMOTED or already CANCELLED entries cannot be cancelled."
    )
    public ResponseEntity<WaitlistEntryResponse> cancelWaitlistEntry(@PathVariable UUID waitlistEntryId) {
        return ResponseEntity.ok(waitlistService.cancelWaitlistEntry(waitlistEntryId));
    }
}
