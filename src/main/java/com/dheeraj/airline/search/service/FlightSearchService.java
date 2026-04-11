package com.dheeraj.airline.search.service;

import com.dheeraj.airline.search.dto.FlightSearchRequest;
import com.dheeraj.airline.search.dto.FlightSearchResponse;
import java.util.List;

public interface FlightSearchService {

    List<FlightSearchResponse> searchFlights(FlightSearchRequest request);
}
