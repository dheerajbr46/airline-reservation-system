package com.dheeraj.airline.search.controller;

import com.dheeraj.airline.search.dto.FlightSearchRequest;
import com.dheeraj.airline.search.dto.FlightSearchResponse;
import com.dheeraj.airline.search.service.FlightSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/flights")
@Tag(name = "Flight Search", description = "Flight discovery by route and date")
public class FlightSearchController {

    private final FlightSearchService flightSearchService;

    public FlightSearchController(FlightSearchService flightSearchService) {
        this.flightSearchService = flightSearchService;
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search flights",
            description = "Searches scheduled flights by origin, destination, and departure date using query parameters."
    )
    public ResponseEntity<List<FlightSearchResponse>> searchFlights(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate
    ) {
        FlightSearchRequest request = FlightSearchRequest.builder()
                .origin(origin)
                .destination(destination)
                .departureDate(departureDate)
                .build();

        return ResponseEntity.ok(flightSearchService.searchFlights(request));
    }

    @PostMapping("/search")
    @Operation(
            summary = "Search flights with request body",
            description = "Searches scheduled flights by origin, destination, and departure date using a JSON request body."
    )
    public ResponseEntity<List<FlightSearchResponse>> searchFlightsWithRequestBody(
            @Valid @RequestBody FlightSearchRequest request
    ) {
        return ResponseEntity.ok(flightSearchService.searchFlights(request));
    }
}
