package com.dheeraj.airline.booking.mapper;

import com.dheeraj.airline.booking.dto.CancelBookingResponse;
import com.dheeraj.airline.booking.dto.BookingResponse;
import com.dheeraj.airline.booking.entity.Booking;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .bookingId(booking.getId())
                .flightId(booking.getFlightId())
                .seatId(booking.getSeatId())
                .holdId(booking.getHoldId())
                .bookingReference(booking.getBookingReference())
                .status(booking.getStatus())
                .bookedAt(booking.getBookedAt())
                .cancelledAt(booking.getCancelledAt())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }

    public CancelBookingResponse toCancelResponse(Booking booking) {
        return CancelBookingResponse.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .status(booking.getStatus())
                .cancelledAt(booking.getCancelledAt())
                .build();
    }
}
