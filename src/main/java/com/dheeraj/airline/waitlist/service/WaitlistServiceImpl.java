package com.dheeraj.airline.waitlist.service;

import com.dheeraj.airline.common.exception.BusinessRuleViolationException;
import com.dheeraj.airline.common.exception.FlightNotFoundException;
import com.dheeraj.airline.common.exception.ResourceNotFoundException;
import com.dheeraj.airline.common.util.IdGenerator;
import com.dheeraj.airline.flight.repository.FlightRepository;
import com.dheeraj.airline.hold.dto.SeatHoldResponse;
import com.dheeraj.airline.hold.entity.HoldStatus;
import com.dheeraj.airline.hold.entity.SeatHold;
import com.dheeraj.airline.hold.mapper.HoldMapper;
import com.dheeraj.airline.hold.repository.SeatHoldRepository;
import com.dheeraj.airline.seat.entity.Seat;
import com.dheeraj.airline.seat.entity.SeatStatus;
import com.dheeraj.airline.seat.repository.SeatRepository;
import com.dheeraj.airline.waitlist.dto.JoinWaitlistRequest;
import com.dheeraj.airline.waitlist.dto.PromoteWaitlistResponse;
import com.dheeraj.airline.waitlist.dto.WaitlistEntryResponse;
import com.dheeraj.airline.waitlist.entity.WaitlistEntry;
import com.dheeraj.airline.waitlist.entity.WaitlistStatus;
import com.dheeraj.airline.waitlist.repository.WaitlistEntryRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WaitlistServiceImpl implements WaitlistService {

    private static final Logger log = LoggerFactory.getLogger(WaitlistServiceImpl.class);

    private static final long PROMOTED_HOLD_DURATION_MINUTES = 10L;

    private final WaitlistEntryRepository waitlistEntryRepository;
    private final FlightRepository flightRepository;
    private final SeatRepository seatRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final HoldMapper holdMapper;
    private final IdGenerator idGenerator;

    public WaitlistServiceImpl(
            WaitlistEntryRepository waitlistEntryRepository,
            FlightRepository flightRepository,
            SeatRepository seatRepository,
            SeatHoldRepository seatHoldRepository,
            HoldMapper holdMapper,
            IdGenerator idGenerator
    ) {
        this.waitlistEntryRepository = waitlistEntryRepository;
        this.flightRepository = flightRepository;
        this.seatRepository = seatRepository;
        this.seatHoldRepository = seatHoldRepository;
        this.holdMapper = holdMapper;
        this.idGenerator = idGenerator;
    }

    @Override
    @Transactional
    public WaitlistEntryResponse joinWaitlist(JoinWaitlistRequest request) {
        if (!flightRepository.existsById(request.getFlightId())) {
            throw new FlightNotFoundException(request.getFlightId());
        }

        if (waitlistEntryRepository.existsByFlightIdAndPassengerEmailIgnoreCaseAndStatus(
                request.getFlightId(),
                request.getPassengerEmail(),
                WaitlistStatus.ACTIVE
        )) {
            throw new BusinessRuleViolationException(
                    "Passenger already has an active waitlist entry for this flight"
            );
        }

        WaitlistEntry waitlistEntry = new WaitlistEntry();
        waitlistEntry.setFlightId(request.getFlightId());
        waitlistEntry.setPassengerName(request.getPassengerName());
        waitlistEntry.setPassengerEmail(request.getPassengerEmail());
        waitlistEntry.setPreferredCabinClass(request.getPreferredCabinClass());
        waitlistEntry.setStatus(WaitlistStatus.ACTIVE);
        waitlistEntry.setQueuePosition(waitlistEntryRepository.findMaxQueuePositionByFlightId(request.getFlightId()) + 1);

        WaitlistEntry savedEntry = waitlistEntryRepository.save(waitlistEntry);
        log.info(
                "Waitlist entry created waitlistEntryId={} flightId={} preferredCabinClass={} queuePosition={} status={}",
                savedEntry.getId(),
                savedEntry.getFlightId(),
                savedEntry.getPreferredCabinClass(),
                savedEntry.getQueuePosition(),
                savedEntry.getStatus()
        );

        return toResponse(savedEntry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WaitlistEntryResponse> getWaitlistForFlight(UUID flightId) {
        if (!flightRepository.existsById(flightId)) {
            throw new FlightNotFoundException(flightId);
        }

        return waitlistEntryRepository.findByFlightIdAndStatusOrderByQueuePositionAsc(
                        flightId,
                        WaitlistStatus.ACTIVE
                ).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PromoteWaitlistResponse promoteNext(UUID flightId) {
        if (!flightRepository.existsById(flightId)) {
            throw new FlightNotFoundException(flightId);
        }

        List<WaitlistEntry> activeEntries = waitlistEntryRepository.findByFlightIdAndStatusOrderByQueuePositionAsc(
                flightId,
                WaitlistStatus.ACTIVE
        );

        if (activeEntries.isEmpty()) {
            throw new BusinessRuleViolationException("No active waitlist entries found for this flight");
        }

        for (WaitlistEntry waitlistEntry : activeEntries) {
            Optional<Seat> availableSeat = findAvailableSeatForPromotion(waitlistEntry);
            if (availableSeat.isPresent()) {
                return promoteWaitlistEntry(waitlistEntry, availableSeat.get());
            }
        }

        throw new BusinessRuleViolationException("No available seat found for active waitlist entries");
    }

    @Override
    @Transactional
    public WaitlistEntryResponse cancelWaitlistEntry(UUID waitlistEntryId) {
        WaitlistEntry waitlistEntry = waitlistEntryRepository.findById(waitlistEntryId)
                .orElseThrow(() -> waitlistEntryNotFound(waitlistEntryId));

        if (waitlistEntry.getStatus() != WaitlistStatus.ACTIVE) {
            throw new BusinessRuleViolationException("Only active waitlist entries can be cancelled");
        }

        waitlistEntry.setStatus(WaitlistStatus.CANCELLED);
        return toResponse(waitlistEntry);
    }

    private Optional<Seat> findAvailableSeatForPromotion(WaitlistEntry waitlistEntry) {
        if (waitlistEntry.getPreferredCabinClass() == null) {
            return seatRepository.findFirstAvailableSeatByFlightIdForUpdate(waitlistEntry.getFlightId());
        }

        return seatRepository.findFirstAvailableSeatByFlightIdAndCabinClassForUpdate(
                waitlistEntry.getFlightId(),
                waitlistEntry.getPreferredCabinClass()
        );
    }

    private PromoteWaitlistResponse promoteWaitlistEntry(WaitlistEntry waitlistEntry, Seat seat) {
        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            throw new BusinessRuleViolationException("Seat is no longer available for waitlist promotion");
        }

        LocalDateTime now = LocalDateTime.now();

        SeatHold seatHold = new SeatHold();
        seatHold.setFlightId(waitlistEntry.getFlightId());
        seatHold.setSeatId(seat.getId());
        seatHold.setHoldReference(idGenerator.generateCode("HOLD"));
        seatHold.setPassengerName(waitlistEntry.getPassengerName());
        seatHold.setPassengerEmail(waitlistEntry.getPassengerEmail());
        seatHold.setStatus(HoldStatus.ACTIVE);
        seatHold.setHeldAt(now);
        seatHold.setExpiresAt(now.plusMinutes(PROMOTED_HOLD_DURATION_MINUTES));

        seat.setStatus(SeatStatus.HELD);
        SeatHold savedHold = seatHoldRepository.save(seatHold);

        waitlistEntry.setStatus(WaitlistStatus.PROMOTED);
        waitlistEntry.setPromotedHoldId(savedHold.getId());
        log.info(
                "Waitlist entry promoted waitlistEntryId={} flightId={} seatId={} promotedHoldId={} queuePosition={} status={}",
                waitlistEntry.getId(),
                waitlistEntry.getFlightId(),
                seat.getId(),
                savedHold.getId(),
                waitlistEntry.getQueuePosition(),
                waitlistEntry.getStatus()
        );

        SeatHoldResponse seatHoldResponse = holdMapper.toResponse(savedHold);
        return PromoteWaitlistResponse.builder()
                .waitlistEntry(toResponse(waitlistEntry))
                .seatHold(seatHoldResponse)
                .build();
    }

    private WaitlistEntryResponse toResponse(WaitlistEntry waitlistEntry) {
        return WaitlistEntryResponse.builder()
                .waitlistEntryId(waitlistEntry.getId())
                .flightId(waitlistEntry.getFlightId())
                .passengerName(waitlistEntry.getPassengerName())
                .passengerEmail(waitlistEntry.getPassengerEmail())
                .preferredCabinClass(waitlistEntry.getPreferredCabinClass())
                .status(waitlistEntry.getStatus())
                .queuePosition(waitlistEntry.getQueuePosition())
                .promotedHoldId(waitlistEntry.getPromotedHoldId())
                .createdAt(waitlistEntry.getCreatedAt())
                .build();
    }

    private ResourceNotFoundException waitlistEntryNotFound(UUID waitlistEntryId) {
        return new ResourceNotFoundException("Waitlist entry not found with id: " + waitlistEntryId);
    }
}
