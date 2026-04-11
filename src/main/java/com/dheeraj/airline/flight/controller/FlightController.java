package com.dheeraj.airline.flight.controller;

import com.dheeraj.airline.flight.dto.CreateFlightRequest;
import com.dheeraj.airline.flight.dto.FlightResponse;
import com.dheeraj.airline.flight.service.FlightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/flights")
@Tag(name = "Flights", description = "Flight inventory management")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @PostMapping
    @Operation(
            summary = "Create flight",
            description = "Creates a scheduled flight with route, aircraft code, and departure/arrival times."
    )
    public ResponseEntity<FlightResponse> createFlight(@Valid @RequestBody CreateFlightRequest request) {
        FlightResponse response = flightService.createFlight(request);
        return ResponseEntity
                .created(URI.create("/api/v1/flights/" + response.getFlightId()))
                .body(response);
    }

    @GetMapping("/{flightId}")
    @Operation(
            summary = "Get flight by id",
            description = "Returns a single flight by its UUID."
    )
    public ResponseEntity<FlightResponse> getFlightById(@PathVariable UUID flightId) {
        return ResponseEntity.ok(flightService.getFlightById(flightId));
    }
}
