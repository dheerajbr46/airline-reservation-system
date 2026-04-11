package com.dheeraj.airline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dheeraj.airline.booking.entity.Booking;
import com.dheeraj.airline.booking.entity.BookingStatus;
import com.dheeraj.airline.booking.repository.BookingRepository;
import com.dheeraj.airline.common.exception.BusinessRuleViolationException;
import com.dheeraj.airline.flight.repository.FlightRepository;
import com.dheeraj.airline.hold.dto.HoldSeatRequest;
import com.dheeraj.airline.hold.entity.HoldStatus;
import com.dheeraj.airline.hold.entity.SeatHold;
import com.dheeraj.airline.hold.repository.SeatHoldRepository;
import com.dheeraj.airline.hold.service.HoldService;
import com.dheeraj.airline.seat.entity.Seat;
import com.dheeraj.airline.seat.entity.SeatStatus;
import com.dheeraj.airline.seat.repository.SeatRepository;
import com.dheeraj.airline.waitlist.entity.WaitlistEntry;
import com.dheeraj.airline.waitlist.entity.WaitlistStatus;
import com.dheeraj.airline.waitlist.repository.WaitlistEntryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AirlineReservationSystemIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private SeatHoldRepository seatHoldRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private WaitlistEntryRepository waitlistEntryRepository;

    @Autowired
    private HoldService holdService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        waitlistEntryRepository.deleteAll();
        seatHoldRepository.deleteAll();
        seatRepository.deleteAll();
        flightRepository.deleteAll();
    }

    @Test
    void createFlight() throws Exception {
        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flightNumber": "AI101",
                                  "aircraftCode": "A320",
                                  "origin": "DEL",
                                  "destination": "BOM",
                                  "departureTime": "2030-05-10T10:00:00",
                                  "arrivalTime": "2030-05-10T12:00:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flightNumber").value("AI101"))
                .andExpect(jsonPath("$.aircraftCode").value("A320"))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        assertThat(flightRepository.findByFlightNumber("AI101")).isPresent();
    }

    @Test
    void createSeats() throws Exception {
        UUID flightId = createFlightAndReturnId("AI102");

        mockMvc.perform(post("/api/v1/seats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flightId": "%s",
                                  "seatNumber": "1A",
                                  "cabinClass": "BUSINESS"
                                }
                                """.formatted(flightId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flightId").value(flightId.toString()))
                .andExpect(jsonPath("$.seatNumber").value("1A"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        List<Seat> seats = seatRepository.findByFlight_IdOrderBySeatNumberAsc(flightId);
        assertThat(seats).hasSize(1);
    }

    @Test
    void searchFlights() throws Exception {
        createFlight("AI103", "A320", "DEL", "BOM", LocalDateTime.of(2030, 5, 11, 9, 0), LocalDateTime.of(2030, 5, 11, 11, 0));
        createFlight("AI104", "B737", "DEL", "BOM", LocalDateTime.of(2030, 5, 11, 7, 0), LocalDateTime.of(2030, 5, 11, 9, 0));
        createFlight("AI105", "A321", "DEL", "BLR", LocalDateTime.of(2030, 5, 11, 8, 0), LocalDateTime.of(2030, 5, 11, 10, 0));

        mockMvc.perform(get("/api/v1/flights/search")
                        .param("origin", "DEL")
                        .param("destination", "BOM")
                        .param("departureDate", "2030-05-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].flightNumber").value("AI104"))
                .andExpect(jsonPath("$[0].aircraftCode").value("B737"))
                .andExpect(jsonPath("$[1].flightNumber").value("AI103"));
    }

    @Test
    void holdSeatSuccessfully() throws Exception {
        UUID flightId = createFlightAndReturnId("AI106");
        UUID seatId = createSeatAndReturnId(flightId, "2A");

        MvcResult result = mockMvc.perform(post("/api/v1/holds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flightId": "%s",
                                  "seatId": "%s",
                                  "passengerName": "Dheeraj Reddy",
                                  "passengerEmail": "dheeraj@example.com"
                                }
                                """.formatted(flightId, seatId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.seatId").value(seatId.toString()))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID holdId = UUID.fromString(response.get("id").asText());

        Seat seat = seatRepository.findById(seatId).orElseThrow();
        SeatHold hold = seatHoldRepository.findById(holdId).orElseThrow();

        assertThat(seat.getStatus()).isEqualTo(SeatStatus.HELD);
        assertThat(hold.getStatus()).isEqualTo(HoldStatus.ACTIVE);
    }

    @Test
    void cannotHoldTheSameSeatTwiceConcurrently() throws Exception {
        UUID flightId = createFlightAndReturnId("AI107");
        UUID seatId = createSeatAndReturnId(flightId, "3A");

        HoldSeatRequest request = HoldSeatRequest.builder()
                .flightId(flightId)
                .seatId(seatId)
                .passengerName("Dheeraj Reddy")
                .passengerEmail("dheeraj@example.com")
                .build();

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            Callable<UUID> task = () -> holdService.placeHold(request).getId();

            Future<UUID> first = executorService.submit(task);
            Future<UUID> second = executorService.submit(task);

            int successCount = 0;
            int failureCount = 0;

            successCount += countSuccess(first);
            successCount += countSuccess(second);
            failureCount += countFailure(first);
            failureCount += countFailure(second);

            assertThat(successCount).isEqualTo(1);
            assertThat(failureCount).isEqualTo(1);
            assertThat(seatHoldRepository.findBySeatIdAndStatusAndNotExpired(seatId, HoldStatus.ACTIVE, LocalDateTime.now()))
                    .hasSize(1);
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void cannotHoldASeatThatIsAlreadyHeld() throws Exception {
        UUID flightId = createFlightAndReturnId("AI114");
        UUID seatId = createSeatAndReturnId(flightId, "10A");
        UUID holdId = placeHoldAndReturnId(flightId, seatId);

        mockMvc.perform(post("/api/v1/holds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flightId": "%s",
                                  "seatId": "%s",
                                  "passengerName": "Another Passenger",
                                  "passengerEmail": "another@example.com"
                                }
                                """.formatted(flightId, seatId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Seat is not available for hold"));

        Seat seat = seatRepository.findById(seatId).orElseThrow();
        SeatHold activeHold = seatHoldRepository.findById(holdId).orElseThrow();

        assertThat(seat.getStatus()).isEqualTo(SeatStatus.HELD);
        assertThat(activeHold.getStatus()).isEqualTo(HoldStatus.ACTIVE);
        assertThat(seatHoldRepository.findBySeatIdAndStatusAndNotExpired(seatId, HoldStatus.ACTIVE, LocalDateTime.now()))
                .hasSize(1);
    }

    @Test
    void confirmBookingFromActiveHold() throws Exception {
        UUID flightId = createFlightAndReturnId("AI108");
        UUID seatId = createSeatAndReturnId(flightId, "4A");
        UUID holdId = placeHoldAndReturnId(flightId, seatId);

        mockMvc.perform(post("/api/v1/bookings/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "holdId": "%s"
                                }
                                """.formatted(holdId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.holdId").value(holdId.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.bookingReference").value(org.hamcrest.Matchers.startsWith("BOOK-")));

        Seat seat = seatRepository.findById(seatId).orElseThrow();
        SeatHold hold = seatHoldRepository.findById(holdId).orElseThrow();
        Booking booking = bookingRepository.findByHoldId(holdId).orElseThrow();

        assertThat(seat.getStatus()).isEqualTo(SeatStatus.BOOKED);
        assertThat(hold.getStatus()).isEqualTo(HoldStatus.CONFIRMED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void listBookingsFilteredByPassengerEmail() throws Exception {
        UUID flightId = createFlightAndReturnId("AI116");
        UUID firstSeatId = createSeatAndReturnId(flightId, "12A");
        UUID secondSeatId = createSeatAndReturnId(flightId, "12B");
        UUID matchingHoldId = placeHoldAndReturnId(flightId, firstSeatId, "dheeraj@example.com");
        UUID otherHoldId = placeHoldAndReturnId(flightId, secondSeatId, "other@example.com");
        UUID matchingBookingId = confirmBookingAndReturnId(matchingHoldId);
        confirmBookingAndReturnId(otherHoldId);

        mockMvc.perform(get("/api/v1/bookings")
                        .param("passengerEmail", "dheeraj@example.com")
                        .param("sortBy", "bookedAt")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].bookingId").value(matchingBookingId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void listBookingsFilteredByFlightId() throws Exception {
        UUID matchingFlightId = createFlightAndReturnId("AI117");
        UUID otherFlightId = createFlightAndReturnId("AI118");
        UUID matchingSeatId = createSeatAndReturnId(matchingFlightId, "13A");
        UUID otherSeatId = createSeatAndReturnId(otherFlightId, "13A");
        UUID matchingHoldId = placeHoldAndReturnId(matchingFlightId, matchingSeatId, "dheeraj@example.com");
        UUID otherHoldId = placeHoldAndReturnId(otherFlightId, otherSeatId, "other@example.com");
        UUID matchingBookingId = confirmBookingAndReturnId(matchingHoldId);
        confirmBookingAndReturnId(otherHoldId);

        mockMvc.perform(get("/api/v1/bookings")
                        .param("flightId", matchingFlightId.toString())
                        .param("sortBy", "bookedAt")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].bookingId").value(matchingBookingId.toString()))
                .andExpect(jsonPath("$.content[0].flightId").value(matchingFlightId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listBookingsFilteredByStatus() throws Exception {
        UUID flightId = createFlightAndReturnId("AI119");
        UUID firstSeatId = createSeatAndReturnId(flightId, "14A");
        UUID secondSeatId = createSeatAndReturnId(flightId, "14B");
        UUID confirmedHoldId = placeHoldAndReturnId(flightId, firstSeatId, "confirmed@example.com");
        UUID cancelledHoldId = placeHoldAndReturnId(flightId, secondSeatId, "cancelled@example.com");
        UUID confirmedBookingId = confirmBookingAndReturnId(confirmedHoldId);
        UUID cancelledBookingId = confirmBookingAndReturnId(cancelledHoldId);

        mockMvc.perform(post("/api/v1/bookings/{bookingId}/cancel", cancelledBookingId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/bookings")
                        .param("status", "CONFIRMED")
                        .param("sortBy", "bookedAt")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].bookingId").value(confirmedBookingId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listHoldsFilteredByPassengerEmail() throws Exception {
        UUID flightId = createFlightAndReturnId("AI120");
        UUID firstSeatId = createSeatAndReturnId(flightId, "15A");
        UUID secondSeatId = createSeatAndReturnId(flightId, "15B");
        UUID matchingHoldId = placeHoldAndReturnId(flightId, firstSeatId, "dheeraj@example.com");
        placeHoldAndReturnId(flightId, secondSeatId, "other@example.com");

        mockMvc.perform(get("/api/v1/holds")
                        .param("passengerEmail", "dheeraj@example.com")
                        .param("sortBy", "createdAt")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(matchingHoldId.toString()))
                .andExpect(jsonPath("$.content[0].passengerEmail").value("dheeraj@example.com"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listHoldsFilteredByFlightId() throws Exception {
        UUID matchingFlightId = createFlightAndReturnId("AI121");
        UUID otherFlightId = createFlightAndReturnId("AI122");
        UUID matchingSeatId = createSeatAndReturnId(matchingFlightId, "16A");
        UUID otherSeatId = createSeatAndReturnId(otherFlightId, "16A");
        UUID matchingHoldId = placeHoldAndReturnId(matchingFlightId, matchingSeatId, "dheeraj@example.com");
        placeHoldAndReturnId(otherFlightId, otherSeatId, "other@example.com");

        mockMvc.perform(get("/api/v1/holds")
                        .param("flightId", matchingFlightId.toString())
                        .param("sortBy", "createdAt")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(matchingHoldId.toString()))
                .andExpect(jsonPath("$.content[0].flightId").value(matchingFlightId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listHoldsFilteredByStatus() throws Exception {
        UUID flightId = createFlightAndReturnId("AI123");
        UUID activeSeatId = createSeatAndReturnId(flightId, "17A");
        UUID releasedSeatId = createSeatAndReturnId(flightId, "17B");
        UUID activeHoldId = placeHoldAndReturnId(flightId, activeSeatId, "active@example.com");
        UUID releasedHoldId = placeHoldAndReturnId(flightId, releasedSeatId, "released@example.com");

        mockMvc.perform(post("/api/v1/holds/{holdId}/release", releasedHoldId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/holds")
                        .param("status", "ACTIVE")
                        .param("sortBy", "createdAt")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(activeHoldId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void holdHistoryPaginationReturnsExpectedPageMetadata() throws Exception {
        UUID flightId = createFlightAndReturnId("AI124");
        UUID firstSeatId = createSeatAndReturnId(flightId, "18A");
        UUID secondSeatId = createSeatAndReturnId(flightId, "18B");
        UUID thirdSeatId = createSeatAndReturnId(flightId, "18C");
        placeHoldAndReturnId(flightId, firstSeatId, "first@example.com");
        placeHoldAndReturnId(flightId, secondSeatId, "second@example.com");
        placeHoldAndReturnId(flightId, thirdSeatId, "third@example.com");

        mockMvc.perform(get("/api/v1/holds")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortBy", "createdAt")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void cannotConfirmExpiredHold() throws Exception {
        UUID flightId = createFlightAndReturnId("AI109");
        UUID seatId = createSeatAndReturnId(flightId, "5A");
        UUID holdId = placeHoldAndReturnId(flightId, seatId);

        SeatHold hold = seatHoldRepository.findById(holdId).orElseThrow();
        hold.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        seatHoldRepository.save(hold);

        mockMvc.perform(post("/api/v1/bookings/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "holdId": "%s"
                                }
                                """.formatted(holdId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Hold has expired and cannot be confirmed"));

        assertThat(bookingRepository.findByHoldId(holdId)).isEmpty();
    }

    @Test
    void cannotConfirmReleasedHold() throws Exception {
        UUID flightId = createFlightAndReturnId("AI112");
        UUID seatId = createSeatAndReturnId(flightId, "8A");
        UUID holdId = placeHoldAndReturnId(flightId, seatId);

        mockMvc.perform(post("/api/v1/holds/{holdId}/release", holdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASED"));

        mockMvc.perform(post("/api/v1/bookings/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "holdId": "%s"
                                }
                                """.formatted(holdId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Released holds cannot be confirmed"));

        Seat seat = seatRepository.findById(seatId).orElseThrow();
        SeatHold hold = seatHoldRepository.findById(holdId).orElseThrow();

        assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(hold.getStatus()).isEqualTo(HoldStatus.RELEASED);
        assertThat(bookingRepository.findByHoldId(holdId)).isEmpty();
    }

    @Test
    void cannotReleaseConfirmedHold() throws Exception {
        UUID flightId = createFlightAndReturnId("AI113");
        UUID seatId = createSeatAndReturnId(flightId, "9A");
        UUID holdId = placeHoldAndReturnId(flightId, seatId);

        confirmBookingAndReturnId(holdId);

        mockMvc.perform(post("/api/v1/holds/{holdId}/release", holdId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Confirmed holds cannot be released"));

        Seat seat = seatRepository.findById(seatId).orElseThrow();
        SeatHold hold = seatHoldRepository.findById(holdId).orElseThrow();

        assertThat(seat.getStatus()).isEqualTo(SeatStatus.BOOKED);
        assertThat(hold.getStatus()).isEqualTo(HoldStatus.CONFIRMED);
    }

    @Test
    void cancelBookingRestoresSeatAvailability() throws Exception {
        UUID flightId = createFlightAndReturnId("AI110");
        UUID seatId = createSeatAndReturnId(flightId, "6A");
        UUID holdId = placeHoldAndReturnId(flightId, seatId);
        UUID bookingId = confirmBookingAndReturnId(holdId);

        mockMvc.perform(post("/api/v1/bookings/{bookingId}/cancel", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        Seat seat = seatRepository.findById(seatId).orElseThrow();
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();

        assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancelledAt()).isNotNull();
    }

    @Test
    void cannotCancelAnAlreadyCancelledBooking() throws Exception {
        UUID flightId = createFlightAndReturnId("AI115");
        UUID seatId = createSeatAndReturnId(flightId, "11A");
        UUID holdId = placeHoldAndReturnId(flightId, seatId);
        UUID bookingId = confirmBookingAndReturnId(holdId);

        mockMvc.perform(post("/api/v1/bookings/{bookingId}/cancel", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/api/v1/bookings/{bookingId}/cancel", bookingId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Booking has already been cancelled"));

        Seat seat = seatRepository.findById(seatId).orElseThrow();
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();

        assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getCancelledAt()).isNotNull();
    }

    @Test
    void releaseExpiredHoldsReturnsSeatsToAvailable() throws Exception {
        UUID flightId = createFlightAndReturnId("AI111");
        UUID seatId = createSeatAndReturnId(flightId, "7A");
        UUID holdId = placeHoldAndReturnId(flightId, seatId);

        SeatHold hold = seatHoldRepository.findById(holdId).orElseThrow();
        hold.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        seatHoldRepository.save(hold);

        mockMvc.perform(post("/api/v1/holds/release-expired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(holdId.toString()))
                .andExpect(jsonPath("$[0].status").value("EXPIRED"));

        Seat seat = seatRepository.findById(seatId).orElseThrow();
        SeatHold expiredHold = seatHoldRepository.findById(holdId).orElseThrow();

        assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(expiredHold.getStatus()).isEqualTo(HoldStatus.EXPIRED);
    }

    @Test
    void joinWaitlistSuccessfully() throws Exception {
        UUID flightId = createFlightAndReturnId("AI125");

        mockMvc.perform(post("/api/v1/waitlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flightId": "%s",
                                  "passengerName": "Dheeraj Reddy",
                                  "passengerEmail": "dheeraj@example.com",
                                  "preferredCabinClass": "ECONOMY"
                                }
                                """.formatted(flightId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flightId").value(flightId.toString()))
                .andExpect(jsonPath("$.passengerEmail").value("dheeraj@example.com"))
                .andExpect(jsonPath("$.preferredCabinClass").value("ECONOMY"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.queuePosition").value(1));

        List<WaitlistEntry> entries = waitlistEntryRepository.findByFlightIdAndStatusOrderByQueuePositionAsc(
                flightId,
                WaitlistStatus.ACTIVE
        );

        assertThat(entries).hasSize(1);
        assertThat(entries.getFirst().getPassengerEmail()).isEqualTo("dheeraj@example.com");
    }

    @Test
    void cannotJoinWaitlistTwiceForSameFlightWhileActive() throws Exception {
        UUID flightId = createFlightAndReturnId("AI126");
        joinWaitlistAndReturnId(flightId, "dheeraj@example.com");

        mockMvc.perform(post("/api/v1/waitlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flightId": "%s",
                                  "passengerName": "Dheeraj Reddy",
                                  "passengerEmail": "dheeraj@example.com",
                                  "preferredCabinClass": "ECONOMY"
                                }
                                """.formatted(flightId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Passenger already has an active waitlist entry for this flight"));

        assertThat(waitlistEntryRepository.findByFlightIdAndStatusOrderByQueuePositionAsc(flightId, WaitlistStatus.ACTIVE))
                .hasSize(1);
    }

    @Test
    void promoteNextCreatesSeatHoldAndMarksEntryPromoted() throws Exception {
        UUID flightId = createFlightAndReturnId("AI127");
        UUID seatId = createSeatAndReturnId(flightId, "19A");
        UUID waitlistEntryId = joinWaitlistAndReturnId(flightId, "dheeraj@example.com");

        MvcResult result = mockMvc.perform(post("/api/v1/flights/{flightId}/waitlist/promote-next", flightId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waitlistEntry.waitlistEntryId").value(waitlistEntryId.toString()))
                .andExpect(jsonPath("$.waitlistEntry.status").value("PROMOTED"))
                .andExpect(jsonPath("$.seatHold.status").value("ACTIVE"))
                .andExpect(jsonPath("$.seatHold.flightId").value(flightId.toString()))
                .andExpect(jsonPath("$.seatHold.seatId").value(seatId.toString()))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID promotedHoldId = UUID.fromString(response.get("waitlistEntry").get("promotedHoldId").asText());

        WaitlistEntry waitlistEntry = waitlistEntryRepository.findById(waitlistEntryId).orElseThrow();
        Seat seat = seatRepository.findById(seatId).orElseThrow();
        SeatHold hold = seatHoldRepository.findById(promotedHoldId).orElseThrow();

        assertThat(waitlistEntry.getStatus()).isEqualTo(WaitlistStatus.PROMOTED);
        assertThat(waitlistEntry.getPromotedHoldId()).isEqualTo(promotedHoldId);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.HELD);
        assertThat(hold.getStatus()).isEqualTo(HoldStatus.ACTIVE);
    }

    @Test
    void promotionRespectsPreferredCabinClass() throws Exception {
        UUID flightId = createFlightAndReturnId("AI128");
        UUID economySeatId = createSeatAndReturnId(flightId, "20A", "ECONOMY");
        UUID businessSeatId = createSeatAndReturnId(flightId, "1A", "BUSINESS");
        joinWaitlistAndReturnId(flightId, "business@example.com", "BUSINESS");

        mockMvc.perform(post("/api/v1/flights/{flightId}/waitlist/promote-next", flightId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seatHold.seatId").value(businessSeatId.toString()))
                .andExpect(jsonPath("$.waitlistEntry.preferredCabinClass").value("BUSINESS"))
                .andExpect(jsonPath("$.waitlistEntry.status").value("PROMOTED"));

        Seat economySeat = seatRepository.findById(economySeatId).orElseThrow();
        Seat businessSeat = seatRepository.findById(businessSeatId).orElseThrow();

        assertThat(economySeat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(businessSeat.getStatus()).isEqualTo(SeatStatus.HELD);
    }

    @Test
    void cancelActiveWaitlistEntryWorks() throws Exception {
        UUID flightId = createFlightAndReturnId("AI129");
        UUID waitlistEntryId = joinWaitlistAndReturnId(flightId, "dheeraj@example.com");

        mockMvc.perform(post("/api/v1/waitlist/{waitlistEntryId}/cancel", waitlistEntryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waitlistEntryId").value(waitlistEntryId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        WaitlistEntry waitlistEntry = waitlistEntryRepository.findById(waitlistEntryId).orElseThrow();

        assertThat(waitlistEntry.getStatus()).isEqualTo(WaitlistStatus.CANCELLED);
    }

    @Test
    void cannotCancelAlreadyPromotedOrCancelledWaitlistEntry() throws Exception {
        UUID flightId = createFlightAndReturnId("AI130");
        createSeatAndReturnId(flightId, "21A");
        UUID promotedEntryId = joinWaitlistAndReturnId(flightId, "promoted@example.com");
        UUID cancelledEntryId = joinWaitlistAndReturnId(flightId, "cancelled@example.com");

        mockMvc.perform(post("/api/v1/flights/{flightId}/waitlist/promote-next", flightId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waitlistEntry.waitlistEntryId").value(promotedEntryId.toString()));

        mockMvc.perform(post("/api/v1/waitlist/{waitlistEntryId}/cancel", promotedEntryId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only active waitlist entries can be cancelled"));

        mockMvc.perform(post("/api/v1/waitlist/{waitlistEntryId}/cancel", cancelledEntryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/api/v1/waitlist/{waitlistEntryId}/cancel", cancelledEntryId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only active waitlist entries can be cancelled"));

        assertThat(waitlistEntryRepository.findById(promotedEntryId).orElseThrow().getStatus())
                .isEqualTo(WaitlistStatus.PROMOTED);
        assertThat(waitlistEntryRepository.findById(cancelledEntryId).orElseThrow().getStatus())
                .isEqualTo(WaitlistStatus.CANCELLED);
    }

    @Test
    void promoteNextReturnsAppropriateResponseWhenNoSeatIsAvailable() throws Exception {
        UUID flightId = createFlightAndReturnId("AI131");
        UUID waitlistEntryId = joinWaitlistAndReturnId(flightId, "dheeraj@example.com");

        mockMvc.perform(post("/api/v1/flights/{flightId}/waitlist/promote-next", flightId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No available seat found for active waitlist entries"));

        WaitlistEntry waitlistEntry = waitlistEntryRepository.findById(waitlistEntryId).orElseThrow();

        assertThat(waitlistEntry.getStatus()).isEqualTo(WaitlistStatus.ACTIVE);
        assertThat(waitlistEntry.getPromotedHoldId()).isNull();
    }

    @Test
    void queueOrderIsRespected() throws Exception {
        UUID flightId = createFlightAndReturnId("AI132");
        createSeatAndReturnId(flightId, "22A");
        UUID firstEntryId = joinWaitlistAndReturnId(flightId, "first@example.com");
        UUID secondEntryId = joinWaitlistAndReturnId(flightId, "second@example.com");

        mockMvc.perform(post("/api/v1/flights/{flightId}/waitlist/promote-next", flightId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waitlistEntry.waitlistEntryId").value(firstEntryId.toString()))
                .andExpect(jsonPath("$.waitlistEntry.status").value("PROMOTED"));

        WaitlistEntry firstEntry = waitlistEntryRepository.findById(firstEntryId).orElseThrow();
        WaitlistEntry secondEntry = waitlistEntryRepository.findById(secondEntryId).orElseThrow();

        assertThat(firstEntry.getStatus()).isEqualTo(WaitlistStatus.PROMOTED);
        assertThat(secondEntry.getStatus()).isEqualTo(WaitlistStatus.ACTIVE);
        assertThat(secondEntry.getQueuePosition()).isEqualTo(2);
    }

    private UUID createFlightAndReturnId(String flightNumber) throws Exception {
        return createFlight(
                flightNumber,
                "A320",
                "DEL",
                "BOM",
                LocalDate.of(2030, 5, 10).atTime(10, 0),
                LocalDate.of(2030, 5, 10).atTime(12, 0)
        );
    }

    private UUID createFlight(
            String flightNumber,
            String aircraftCode,
            String origin,
            String destination,
            LocalDateTime departureTime,
            LocalDateTime arrivalTime
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flightNumber": "%s",
                                  "aircraftCode": "%s",
                                  "origin": "%s",
                                  "destination": "%s",
                                  "departureTime": "%s",
                                  "arrivalTime": "%s"
                                }
                                """.formatted(flightNumber, aircraftCode, origin, destination, departureTime, arrivalTime)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(response.get("flightId").asText());
    }

    private UUID createSeatAndReturnId(UUID flightId, String seatNumber) throws Exception {
        return createSeatAndReturnId(flightId, seatNumber, "ECONOMY");
    }

    private UUID createSeatAndReturnId(UUID flightId, String seatNumber, String cabinClass) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/seats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flightId": "%s",
                                  "seatNumber": "%s",
                                  "cabinClass": "%s"
                                }
                                """.formatted(flightId, seatNumber, cabinClass)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(response.get("seatId").asText());
    }

    private UUID placeHoldAndReturnId(UUID flightId, UUID seatId) throws Exception {
        return placeHoldAndReturnId(flightId, seatId, "dheeraj@example.com");
    }

    private UUID placeHoldAndReturnId(UUID flightId, UUID seatId, String passengerEmail) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/holds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flightId": "%s",
                                  "seatId": "%s",
                                  "passengerName": "Dheeraj Reddy",
                                  "passengerEmail": "%s"
                                }
                                """.formatted(flightId, seatId, passengerEmail)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(response.get("id").asText());
    }

    private UUID joinWaitlistAndReturnId(UUID flightId, String passengerEmail) throws Exception {
        return joinWaitlistAndReturnId(flightId, passengerEmail, "ECONOMY");
    }

    private UUID joinWaitlistAndReturnId(UUID flightId, String passengerEmail, String preferredCabinClass) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/waitlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "flightId": "%s",
                                  "passengerName": "Dheeraj Reddy",
                                  "passengerEmail": "%s",
                                  "preferredCabinClass": "%s"
                                }
                                """.formatted(flightId, passengerEmail, preferredCabinClass)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(response.get("waitlistEntryId").asText());
    }

    private UUID confirmBookingAndReturnId(UUID holdId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/bookings/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "holdId": "%s"
                                }
                                """.formatted(holdId)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(response.get("bookingId").asText());
    }

    private int countSuccess(Future<UUID> future) {
        try {
            future.get();
            return 1;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(exception);
        } catch (ExecutionException exception) {
            return 0;
        }
    }

    private int countFailure(Future<UUID> future) {
        try {
            future.get();
            return 0;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(exception);
        } catch (ExecutionException exception) {
            assertThat(exception.getCause()).isInstanceOf(BusinessRuleViolationException.class);
            return 1;
        }
    }
}
