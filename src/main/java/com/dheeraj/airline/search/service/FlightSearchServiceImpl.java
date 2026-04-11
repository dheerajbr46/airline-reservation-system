package com.dheeraj.airline.search.service;

import com.dheeraj.airline.flight.repository.FlightRepository;
import com.dheeraj.airline.search.dto.FlightSearchRequest;
import com.dheeraj.airline.search.dto.FlightSearchResponse;
import com.dheeraj.airline.search.mapper.FlightSearchMapper;
import com.dheeraj.airline.seat.entity.SeatStatus;
import com.dheeraj.airline.seat.repository.SeatRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FlightSearchServiceImpl implements FlightSearchService {

    private final FlightRepository flightRepository;
    private final SeatRepository seatRepository;
    private final FlightSearchMapper flightSearchMapper;

    public FlightSearchServiceImpl(
            FlightRepository flightRepository,
            SeatRepository seatRepository,
            FlightSearchMapper flightSearchMapper
    ) {
        this.flightRepository = flightRepository;
        this.seatRepository = seatRepository;
        this.flightSearchMapper = flightSearchMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlightSearchResponse> searchFlights(FlightSearchRequest request) {
        return flightRepository.searchByOriginDestinationAndDepartureDate(
                        request.getOrigin(),
                        request.getDestination(),
                        request.getDepartureDate()
                ).stream()
                .map(flight -> flightSearchMapper.toResponse(
                        flight,
                        seatRepository.countByFlight_IdAndStatus(flight.getId(), SeatStatus.AVAILABLE)
                ))
                .toList();
    }
}
