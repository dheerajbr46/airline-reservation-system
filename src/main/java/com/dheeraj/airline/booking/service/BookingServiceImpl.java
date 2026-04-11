package com.dheeraj.airline.booking.service;

import com.dheeraj.airline.booking.dto.BookingResponse;
import com.dheeraj.airline.booking.dto.CancelBookingResponse;
import com.dheeraj.airline.booking.dto.ConfirmBookingRequest;
import com.dheeraj.airline.booking.entity.Booking;
import com.dheeraj.airline.booking.entity.BookingStatus;
import com.dheeraj.airline.booking.mapper.BookingMapper;
import com.dheeraj.airline.booking.repository.BookingRepository;
import com.dheeraj.airline.common.exception.BusinessRuleViolationException;
import com.dheeraj.airline.common.exception.ResourceNotFoundException;
import com.dheeraj.airline.common.response.PagedResponse;
import com.dheeraj.airline.common.util.IdGenerator;
import com.dheeraj.airline.hold.entity.HoldStatus;
import com.dheeraj.airline.hold.entity.SeatHold;
import com.dheeraj.airline.hold.repository.SeatHoldRepository;
import com.dheeraj.airline.seat.entity.Seat;
import com.dheeraj.airline.seat.entity.SeatStatus;
import com.dheeraj.airline.seat.repository.SeatRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingServiceImpl implements BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private static final int MAX_BOOKING_REFERENCE_ATTEMPTS = 5;
    private static final Set<String> ALLOWED_BOOKING_SORT_FIELDS = Set.of(
            "bookedAt",
            "cancelledAt",
            "createdAt",
            "updatedAt",
            "bookingReference",
            "status"
    );

    private final BookingRepository bookingRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final SeatRepository seatRepository;
    private final BookingMapper bookingMapper;
    private final IdGenerator idGenerator;

    public BookingServiceImpl(
            BookingRepository bookingRepository,
            SeatHoldRepository seatHoldRepository,
            SeatRepository seatRepository,
            BookingMapper bookingMapper,
            IdGenerator idGenerator
    ) {
        this.bookingRepository = bookingRepository;
        this.seatHoldRepository = seatHoldRepository;
        this.seatRepository = seatRepository;
        this.bookingMapper = bookingMapper;
        this.idGenerator = idGenerator;
    }

    @Override
    @Transactional
    public BookingResponse confirmBooking(ConfirmBookingRequest request) {
        LocalDateTime now = LocalDateTime.now();

        SeatHold seatHold = seatHoldRepository.findByIdForUpdate(request.getHoldId())
                .orElseThrow(() -> seatHoldNotFound(request.getHoldId()));

        validateActiveUnexpiredHold(seatHold, now);

        Seat seat = seatRepository.findByIdForUpdate(seatHold.getSeatId())
                .orElseThrow(() -> seatNotFound(seatHold.getSeatId()));

        if (seat.getStatus() != SeatStatus.HELD) {
            throw new BusinessRuleViolationException("Seat must be in HELD status before confirming booking");
        }

        Booking booking = new Booking();
        booking.setFlightId(seatHold.getFlightId());
        booking.setSeatId(seatHold.getSeatId());
        booking.setHoldId(seatHold.getId());
        booking.setBookingReference(generateUniqueBookingReference());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookedAt(now);

        seat.setStatus(SeatStatus.BOOKED);
        seatHold.setStatus(HoldStatus.CONFIRMED);

        Booking savedBooking = bookingRepository.save(booking);
        log.info(
                "Booking confirmed bookingId={} bookingReference={} holdId={} flightId={} seatId={} status={}",
                savedBooking.getId(),
                savedBooking.getBookingReference(),
                savedBooking.getHoldId(),
                savedBooking.getFlightId(),
                savedBooking.getSeatId(),
                savedBooking.getStatus()
        );

        return bookingMapper.toResponse(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> bookingNotFound(bookingId));

        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<BookingResponse> getBookingHistory(
            String passengerEmail,
            UUID flightId,
            BookingStatus status,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        Pageable pageable = buildBookingHistoryPageable(page, size, sortBy, direction);
        Page<Booking> bookingPage = bookingRepository.findBookingHistory(
                normalizePassengerEmail(passengerEmail),
                flightId,
                status,
                pageable
        );

        List<BookingResponse> content = bookingPage.getContent().stream()
                .map(bookingMapper::toResponse)
                .toList();

        return PagedResponse.<BookingResponse>builder()
                .content(content)
                .page(bookingPage.getNumber())
                .size(bookingPage.getSize())
                .totalElements(bookingPage.getTotalElements())
                .totalPages(bookingPage.getTotalPages())
                .last(bookingPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public CancelBookingResponse cancelBooking(UUID bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> bookingNotFound(bookingId));

        validateBookingCanBeCancelled(booking);

        Seat seat = seatRepository.findByIdForUpdate(booking.getSeatId())
                .orElseThrow(() -> seatNotFound(booking.getSeatId()));

        if (seat.getStatus() != SeatStatus.BOOKED) {
            throw new BusinessRuleViolationException("Seat must be in BOOKED status before cancelling a booking");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        seat.setStatus(SeatStatus.AVAILABLE);
        log.info(
                "Booking cancelled bookingId={} bookingReference={} flightId={} seatId={} status={} seatStatus={}",
                booking.getId(),
                booking.getBookingReference(),
                booking.getFlightId(),
                booking.getSeatId(),
                booking.getStatus(),
                seat.getStatus()
        );

        return bookingMapper.toCancelResponse(booking);
    }

    private Pageable buildBookingHistoryPageable(int page, int size, String sortBy, String direction) {
        if (page < 0) {
            throw new BusinessRuleViolationException("Page index must not be negative");
        }

        if (size < 1) {
            throw new BusinessRuleViolationException("Page size must be at least 1");
        }

        if (!ALLOWED_BOOKING_SORT_FIELDS.contains(sortBy)) {
            throw new BusinessRuleViolationException("Unsupported booking sort field: " + sortBy);
        }

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
    }

    private String normalizePassengerEmail(String passengerEmail) {
        if (passengerEmail == null || passengerEmail.isBlank()) {
            return null;
        }

        return passengerEmail.trim();
    }

    private void validateBookingCanBeCancelled(Booking booking) {
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessRuleViolationException("Booking has already been cancelled");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessRuleViolationException("Only confirmed bookings can be cancelled");
        }
    }

    private void validateActiveUnexpiredHold(SeatHold seatHold, LocalDateTime currentTime) {
        if (seatHold.getStatus() == HoldStatus.RELEASED) {
            throw new BusinessRuleViolationException("Released holds cannot be confirmed");
        }

        if (seatHold.getStatus() == HoldStatus.CONFIRMED) {
            throw new BusinessRuleViolationException("Confirmed holds cannot be confirmed again");
        }

        if (seatHold.getStatus() == HoldStatus.EXPIRED) {
            throw new BusinessRuleViolationException("Expired holds cannot be confirmed");
        }

        if (seatHold.getStatus() != HoldStatus.ACTIVE) {
            throw new BusinessRuleViolationException("Only active holds can be confirmed");
        }

        if (!seatHold.getExpiresAt().isAfter(currentTime)) {
            throw new BusinessRuleViolationException("Hold has expired and cannot be confirmed");
        }
    }

    private String generateUniqueBookingReference() {
        for (int attempt = 0; attempt < MAX_BOOKING_REFERENCE_ATTEMPTS; attempt++) {
            String bookingReference = idGenerator.generateCode("BOOK");
            if (bookingRepository.findByBookingReference(bookingReference).isEmpty()) {
                return bookingReference;
            }
        }

        throw new BusinessRuleViolationException("Unable to generate a unique booking reference");
    }

    private ResourceNotFoundException seatHoldNotFound(UUID holdId) {
        return new ResourceNotFoundException("Seat hold not found with id: " + holdId);
    }

    private ResourceNotFoundException seatNotFound(UUID seatId) {
        return new ResourceNotFoundException("Seat not found with id: " + seatId);
    }

    private ResourceNotFoundException bookingNotFound(UUID bookingId) {
        return new ResourceNotFoundException("Booking not found with id: " + bookingId);
    }
}
