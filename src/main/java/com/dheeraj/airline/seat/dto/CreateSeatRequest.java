package com.dheeraj.airline.seat.dto;

import com.dheeraj.airline.seat.entity.CabinClass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateSeatRequest {

    @NotNull
    private UUID flightId;

    @NotBlank
    @Size(max = 10)
    private String seatNumber;

    @NotNull
    private CabinClass cabinClass;
}
