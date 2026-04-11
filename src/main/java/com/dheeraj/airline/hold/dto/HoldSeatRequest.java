package com.dheeraj.airline.hold.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class HoldSeatRequest {

    @NotNull
    private UUID flightId;

    @NotNull
    private UUID seatId;

    @NotBlank
    private String passengerName;

    @NotBlank
    @Email
    private String passengerEmail;
}
