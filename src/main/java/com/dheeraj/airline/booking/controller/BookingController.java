package com.dheeraj.airline.booking.controller;

import com.dheeraj.airline.booking.dto.BookingResponse;
import com.dheeraj.airline.booking.dto.CancelBookingResponse;
import com.dheeraj.airline.booking.dto.ConfirmBookingRequest;
import com.dheeraj.airline.booking.entity.BookingStatus;
import com.dheeraj.airline.booking.service.BookingService;
import com.dheeraj.airline.common.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Bookings", description = "Booking confirmation, retrieval, history, and cancellation")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/confirm")
    @Operation(
            summary = "Confirm booking from hold",
            description = "Confirms an ACTIVE, unexpired seat hold, marks the seat BOOKED, and creates a booking reference."
    )
    public ResponseEntity<BookingResponse> confirmBooking(@Valid @RequestBody ConfirmBookingRequest request) {
        BookingResponse response = bookingService.confirmBooking(request);
        return ResponseEntity.created(URI.create("/api/v1/bookings/" + response.getBookingId())).body(response);
    }

    @GetMapping("/{bookingId}")
    @Operation(
            summary = "Get booking by id",
            description = "Returns a booking by UUID, including booking reference, seat, flight, hold, and status."
    )
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(bookingService.getBookingById(bookingId));
    }

    @GetMapping
    @Operation(
            summary = "List booking history",
            description = "Returns paginated booking history with optional filters for passenger email, flight, and booking status."
    )
    public ResponseEntity<PagedResponse<BookingResponse>> getBookingHistory(
            @RequestParam(required = false) String passengerEmail,
            @RequestParam(required = false) UUID flightId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "bookedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ResponseEntity.ok(bookingService.getBookingHistory(
                passengerEmail,
                flightId,
                status,
                page,
                size,
                sortBy,
                direction
        ));
    }

    @PostMapping("/{bookingId}/cancel")
    @Operation(
            summary = "Cancel booking",
            description = "Cancels a CONFIRMED booking and restores the booked seat to AVAILABLE. Already cancelled bookings cannot be cancelled again."
    )
    public ResponseEntity<CancelBookingResponse> cancelBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId));
    }
}
