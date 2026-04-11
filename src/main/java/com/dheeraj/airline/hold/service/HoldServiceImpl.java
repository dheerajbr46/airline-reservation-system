package com.dheeraj.airline.hold.service;

import com.dheeraj.airline.common.exception.BusinessRuleViolationException;
import com.dheeraj.airline.common.exception.FlightNotFoundException;
import com.dheeraj.airline.common.exception.ResourceNotFoundException;
import com.dheeraj.airline.common.response.PagedResponse;
import com.dheeraj.airline.common.util.IdGenerator;
import com.dheeraj.airline.flight.repository.FlightRepository;
import com.dheeraj.airline.hold.dto.HoldSeatRequest;
import com.dheeraj.airline.hold.dto.SeatHoldResponse;
import com.dheeraj.airline.hold.entity.HoldStatus;
import com.dheeraj.airline.hold.entity.SeatHold;
import com.dheeraj.airline.hold.mapper.HoldMapper;
import com.dheeraj.airline.hold.repository.SeatHoldRepository;
import com.dheeraj.airline.seat.entity.Seat;
import com.dheeraj.airline.seat.entity.SeatStatus;
import com.dheeraj.airline.seat.repository.SeatRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
public class HoldServiceImpl implements HoldService {

    private static final Logger log = LoggerFactory.getLogger(HoldServiceImpl.class);

    private static final long HOLD_DURATION_MINUTES = 10L;
    private static final Set<String> ALLOWED_HOLD_SORT_FIELDS = Set.of(
            "createdAt",
            "updatedAt",
            "heldAt",
            "expiresAt",
            "holdReference",
            "passengerEmail",
            "status"
    );

    private final SeatHoldRepository seatHoldRepository;
    private final SeatRepository seatRepository;
    private final FlightRepository flightRepository;
    private final HoldMapper holdMapper;
    private final IdGenerator idGenerator;

    public HoldServiceImpl(
            SeatHoldRepository seatHoldRepository,
            SeatRepository seatRepository,
            FlightRepository flightRepository,
            HoldMapper holdMapper,
            IdGenerator idGenerator
    ) {
        this.seatHoldRepository = seatHoldRepository;
        this.seatRepository = seatRepository;
        this.flightRepository = flightRepository;
        this.holdMapper = holdMapper;
        this.idGenerator = idGenerator;
    }

    @Override
    @Transactional
    public SeatHoldResponse placeHold(HoldSeatRequest request) {
        if (!flightRepository.existsById(request.getFlightId())) {
            throw new FlightNotFoundException(request.getFlightId());
        }

        LocalDateTime now = LocalDateTime.now();

        Seat seat = seatRepository.findByIdForUpdate(request.getSeatId())
                .orElseThrow(() -> seatNotFound(request.getSeatId()));

        validateSeatBelongsToFlight(seat, request.getFlightId());

        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            throw new BusinessRuleViolationException("Seat is not available for hold");
        }

        SeatHold seatHold = new SeatHold();
        seatHold.setFlightId(request.getFlightId());
        seatHold.setSeatId(request.getSeatId());
        seatHold.setHoldReference(idGenerator.generateCode("HOLD"));
        seatHold.setPassengerName(request.getPassengerName());
        seatHold.setPassengerEmail(request.getPassengerEmail());
        seatHold.setStatus(HoldStatus.ACTIVE);
        seatHold.setHeldAt(now);
        seatHold.setExpiresAt(now.plusMinutes(HOLD_DURATION_MINUTES));

        seat.setStatus(SeatStatus.HELD);
        SeatHold savedHold = seatHoldRepository.save(seatHold);
        log.info(
                "Seat hold created holdId={} holdReference={} flightId={} seatId={} status={} expiresAt={}",
                savedHold.getId(),
                savedHold.getHoldReference(),
                savedHold.getFlightId(),
                savedHold.getSeatId(),
                savedHold.getStatus(),
                savedHold.getExpiresAt()
        );

        return holdMapper.toResponse(savedHold);
    }

    @Override
    @Transactional
    public SeatHoldResponse releaseHold(UUID holdId) {
        SeatHold seatHold = seatHoldRepository.findByIdForUpdate(holdId)
                .orElseThrow(() -> seatHoldNotFound(holdId));

        validateHoldCanBeReleased(seatHold);

        Seat seat = seatRepository.findByIdForUpdate(seatHold.getSeatId())
                .orElseThrow(() -> seatNotFound(seatHold.getSeatId()));

        if (seat.getStatus() != SeatStatus.HELD) {
            throw new BusinessRuleViolationException("Seat must be in HELD status before releasing a hold");
        }

        seatHold.setStatus(HoldStatus.RELEASED);
        seat.setStatus(SeatStatus.AVAILABLE);
        log.info(
                "Seat hold released holdId={} flightId={} seatId={} status={} seatStatus={}",
                seatHold.getId(),
                seatHold.getFlightId(),
                seatHold.getSeatId(),
                seatHold.getStatus(),
                seat.getStatus()
        );

        return holdMapper.toResponse(seatHold);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<SeatHoldResponse> getHoldHistory(
            String passengerEmail,
            UUID flightId,
            HoldStatus status,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        Pageable pageable = buildHoldHistoryPageable(page, size, sortBy, direction);
        Page<SeatHold> holdPage = seatHoldRepository.findHoldHistory(
                normalizePassengerEmail(passengerEmail),
                flightId,
                status,
                pageable
        );

        List<SeatHoldResponse> content = holdPage.getContent().stream()
                .map(holdMapper::toResponse)
                .toList();

        return PagedResponse.<SeatHoldResponse>builder()
                .content(content)
                .page(holdPage.getNumber())
                .size(holdPage.getSize())
                .totalElements(holdPage.getTotalElements())
                .totalPages(holdPage.getTotalPages())
                .last(holdPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public List<SeatHoldResponse> releaseExpiredHolds() {
        LocalDateTime now = LocalDateTime.now();
        List<SeatHold> expiredHolds = seatHoldRepository.findExpiredHoldsForUpdate(now);
        List<SeatHold> releasedExpiredHolds = new ArrayList<>();

        for (SeatHold seatHold : expiredHolds) {
            if (!isExpiredActiveHold(seatHold, now)) {
                continue;
            }

            Seat lockedSeat = seatRepository.findByIdForUpdate(seatHold.getSeatId())
                    .orElseThrow(() -> seatNotFound(seatHold.getSeatId()));

            restoreSeatForExpiredHold(lockedSeat, seatHold);
            seatHold.setStatus(HoldStatus.EXPIRED);
            releasedExpiredHolds.add(seatHold);
            log.info(
                    "Expired seat hold released holdId={} flightId={} seatId={} status={} seatStatus={}",
                    seatHold.getId(),
                    seatHold.getFlightId(),
                    seatHold.getSeatId(),
                    seatHold.getStatus(),
                    lockedSeat.getStatus()
            );
        }

        log.info("Expired hold release completed releasedCount={}", releasedExpiredHolds.size());

        return releasedExpiredHolds.stream()
                .map(holdMapper::toResponse)
                .toList();
    }

    private Pageable buildHoldHistoryPageable(int page, int size, String sortBy, String direction) {
        if (page < 0) {
            throw new BusinessRuleViolationException("Page index must not be negative");
        }

        if (size < 1) {
            throw new BusinessRuleViolationException("Page size must be at least 1");
        }

        if (!ALLOWED_HOLD_SORT_FIELDS.contains(sortBy)) {
            throw new BusinessRuleViolationException("Unsupported hold sort field: " + sortBy);
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

    private boolean isExpiredActiveHold(SeatHold seatHold, LocalDateTime currentTime) {
        return seatHold.getStatus() == HoldStatus.ACTIVE
                && seatHold.getExpiresAt().isBefore(currentTime);
    }

    private void restoreSeatForExpiredHold(Seat seat, SeatHold seatHold) {
        if (seat.getStatus() == SeatStatus.BOOKED) {
            throw new BusinessRuleViolationException(
                    "Cannot expire hold because seat is already booked for hold id: " + seatHold.getId()
            );
        }

        if (seat.getStatus() == SeatStatus.HELD) {
            seat.setStatus(SeatStatus.AVAILABLE);
        }
    }

    private void validateSeatBelongsToFlight(Seat seat, UUID flightId) {
        if (!seat.getFlight().getId().equals(flightId)) {
            throw new BusinessRuleViolationException("Seat does not belong to the given flight");
        }
    }

    private void validateHoldCanBeReleased(SeatHold seatHold) {
        if (seatHold.getStatus() == HoldStatus.CONFIRMED) {
            throw new BusinessRuleViolationException("Confirmed holds cannot be released");
        }

        if (seatHold.getStatus() == HoldStatus.RELEASED) {
            throw new BusinessRuleViolationException("Hold has already been released");
        }

        if (seatHold.getStatus() == HoldStatus.EXPIRED) {
            throw new BusinessRuleViolationException("Expired holds cannot be released manually");
        }

        if (seatHold.getStatus() != HoldStatus.ACTIVE) {
            throw new BusinessRuleViolationException("Only active holds can be released");
        }
    }

    private ResourceNotFoundException seatNotFound(UUID seatId) {
        return new ResourceNotFoundException("Seat not found with id: " + seatId);
    }

    private ResourceNotFoundException seatHoldNotFound(UUID holdId) {
        return new ResourceNotFoundException("Seat hold not found with id: " + holdId);
    }
}
