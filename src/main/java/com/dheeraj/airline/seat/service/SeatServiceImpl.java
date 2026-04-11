package com.dheeraj.airline.seat.service;

import com.dheeraj.airline.common.exception.FlightNotFoundException;
import com.dheeraj.airline.common.exception.SeatAlreadyExistsException;
import com.dheeraj.airline.flight.entity.Flight;
import com.dheeraj.airline.flight.repository.FlightRepository;
import com.dheeraj.airline.seat.dto.CreateSeatRequest;
import com.dheeraj.airline.seat.dto.SeatResponse;
import com.dheeraj.airline.seat.entity.Seat;
import com.dheeraj.airline.seat.entity.SeatStatus;
import com.dheeraj.airline.seat.mapper.SeatMapper;
import com.dheeraj.airline.seat.repository.SeatRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeatServiceImpl implements SeatService {

    private static final Logger log = LoggerFactory.getLogger(SeatServiceImpl.class);

    private final SeatRepository seatRepository;
    private final FlightRepository flightRepository;
    private final SeatMapper seatMapper;

    public SeatServiceImpl(
            SeatRepository seatRepository,
            FlightRepository flightRepository,
            SeatMapper seatMapper
    ) {
        this.seatRepository = seatRepository;
        this.flightRepository = flightRepository;
        this.seatMapper = seatMapper;
    }

    @Override
    @Transactional
    public SeatResponse createSeat(CreateSeatRequest request) {
        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new FlightNotFoundException(request.getFlightId()));

        seatRepository.findByFlight_IdAndSeatNumber(request.getFlightId(), request.getSeatNumber())
                .ifPresent(existingSeat -> {
                    throw new SeatAlreadyExistsException(request.getFlightId(), request.getSeatNumber());
                });

        Seat seat = seatMapper.toEntity(request, flight);
        seat.setStatus(SeatStatus.AVAILABLE);

        Seat savedSeat = seatRepository.save(seat);
        log.info(
                "Seat created seatId={} flightId={} seatNumber={} cabinClass={} status={}",
                savedSeat.getId(),
                flight.getId(),
                savedSeat.getSeatNumber(),
                savedSeat.getCabinClass(),
                savedSeat.getStatus()
        );

        return seatMapper.toResponse(savedSeat);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByFlight(UUID flightId) {
        validateFlightExists(flightId);

        return seatRepository.findByFlight_IdOrderBySeatNumberAsc(flightId).stream()
                .map(seatMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatResponse> getAvailableSeatsByFlight(UUID flightId) {
        validateFlightExists(flightId);

        return seatRepository.findByFlight_IdAndStatusOrderBySeatNumberAsc(flightId, SeatStatus.AVAILABLE).stream()
                .map(seatMapper::toResponse)
                .toList();
    }

    private void validateFlightExists(UUID flightId) {
        if (!flightRepository.existsById(flightId)) {
            throw new FlightNotFoundException(flightId);
        }
    }
}
