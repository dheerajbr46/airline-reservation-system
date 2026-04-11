package com.dheeraj.airline.seat.controller;

import com.dheeraj.airline.seat.dto.CreateSeatRequest;
import com.dheeraj.airline.seat.dto.SeatResponse;
import com.dheeraj.airline.seat.service.SeatService;
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
@RequestMapping("/api/v1/seats")
@Tag(name = "Seats", description = "Seat inventory and availability")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @PostMapping
    @Operation(
            summary = "Create seat",
            description = "Adds a seat to an existing flight. Seats start as AVAILABLE."
    )
    public ResponseEntity<SeatResponse> createSeat(@Valid @RequestBody CreateSeatRequest request) {
        SeatResponse response = seatService.createSeat(request);
        return ResponseEntity
                .created(URI.create("/api/v1/seats/" + response.getSeatId()))
                .body(response);
    }

    @GetMapping("/flights/{flightId}/seats")
    @Operation(
            summary = "List seats for flight",
            description = "Returns all seats configured for a flight, regardless of current status."
    )
    public ResponseEntity<List<SeatResponse>> getSeatsByFlight(@PathVariable UUID flightId) {
        return ResponseEntity.ok(seatService.getSeatsByFlight(flightId));
    }

    @GetMapping("/flights/{flightId}/seats/available")
    @Operation(
            summary = "List available seats for flight",
            description = "Returns only seats currently available for hold or future booking."
    )
    public ResponseEntity<List<SeatResponse>> getAvailableSeatsByFlight(@PathVariable UUID flightId) {
        return ResponseEntity.ok(seatService.getAvailableSeatsByFlight(flightId));
    }
}
