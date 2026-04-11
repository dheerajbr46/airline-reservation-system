package com.dheeraj.airline.flight.service;

import com.dheeraj.airline.common.exception.FlightAlreadyExistsException;
import com.dheeraj.airline.common.exception.FlightNotFoundException;
import com.dheeraj.airline.flight.dto.CreateFlightRequest;
import com.dheeraj.airline.flight.dto.FlightResponse;
import com.dheeraj.airline.flight.entity.Flight;
import com.dheeraj.airline.flight.entity.FlightStatus;
import com.dheeraj.airline.flight.mapper.FlightMapper;
import com.dheeraj.airline.flight.repository.FlightRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FlightServiceImpl implements FlightService {

    private static final Logger log = LoggerFactory.getLogger(FlightServiceImpl.class);

    private final FlightRepository flightRepository;
    private final FlightMapper flightMapper;

    public FlightServiceImpl(FlightRepository flightRepository, FlightMapper flightMapper) {
        this.flightRepository = flightRepository;
        this.flightMapper = flightMapper;
    }

    @Override
    @Transactional
    public FlightResponse createFlight(CreateFlightRequest request) {
        if (flightRepository.findByFlightNumber(request.getFlightNumber()).isPresent()) {
            throw new FlightAlreadyExistsException(request.getFlightNumber());
        }

        Flight flight = flightMapper.toEntity(request);
        flight.setStatus(FlightStatus.SCHEDULED);

        Flight savedFlight = flightRepository.save(flight);
        log.info(
                "Flight created flightId={} flightNumber={} aircraftCode={} origin={} destination={} departureTime={} status={}",
                savedFlight.getId(),
                savedFlight.getFlightNumber(),
                savedFlight.getAircraftCode(),
                savedFlight.getOrigin(),
                savedFlight.getDestination(),
                savedFlight.getDepartureTime(),
                savedFlight.getStatus()
        );

        return flightMapper.toResponse(savedFlight);
    }

    @Override
    @Transactional(readOnly = true)
    public FlightResponse getFlightById(UUID flightId) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new FlightNotFoundException(flightId));

        return flightMapper.toResponse(flight);
    }
}
