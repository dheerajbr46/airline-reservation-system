package com.dheeraj.airline.booking.dto;

import com.dheeraj.airline.booking.entity.BookingStatus;
import java.time.LocalDateTime;
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
public class CancelBookingResponse {

    private UUID bookingId;
    private String bookingReference;
    private BookingStatus status;
    private LocalDateTime cancelledAt;
}
