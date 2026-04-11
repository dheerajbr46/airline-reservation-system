package com.dheeraj.airline.booking.service;

import com.dheeraj.airline.booking.dto.BookingResponse;
import com.dheeraj.airline.booking.dto.CancelBookingResponse;
import com.dheeraj.airline.booking.dto.ConfirmBookingRequest;
import com.dheeraj.airline.booking.entity.BookingStatus;
import com.dheeraj.airline.common.response.PagedResponse;
import java.util.UUID;

public interface BookingService {

    BookingResponse confirmBooking(ConfirmBookingRequest request);

    BookingResponse getBookingById(UUID bookingId);

    PagedResponse<BookingResponse> getBookingHistory(
            String passengerEmail,
            UUID flightId,
            BookingStatus status,
            int page,
            int size,
            String sortBy,
            String direction
    );

    CancelBookingResponse cancelBooking(UUID bookingId);
}
