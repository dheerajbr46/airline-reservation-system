package com.dheeraj.airline.flight.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
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
public class CreateFlightRequest {

    @NotBlank
    @Size(max = 20)
    private String flightNumber;

    @NotBlank
    @Size(max = 20)
    private String aircraftCode;

    @NotBlank
    @Size(max = 10)
    private String origin;

    @NotBlank
    @Size(max = 10)
    private String destination;

    @NotNull
    @Future
    private LocalDateTime departureTime;

    @NotNull
    @Future
    private LocalDateTime arrivalTime;
}
