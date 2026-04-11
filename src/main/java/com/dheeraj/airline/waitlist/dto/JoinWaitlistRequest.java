package com.dheeraj.airline.waitlist.dto;

import com.dheeraj.airline.seat.entity.CabinClass;
import jakarta.validation.constraints.Email;
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
public class JoinWaitlistRequest {

    @NotNull
    private UUID flightId;

    @NotBlank
    @Size(max = 100)
    private String passengerName;

    @NotBlank
    @Email
    @Size(max = 150)
    private String passengerEmail;

    @NotNull
    private CabinClass preferredCabinClass;
}
