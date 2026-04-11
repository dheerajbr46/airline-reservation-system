ALTER TABLE flights
    ADD CONSTRAINT uk_flights_flight_number UNIQUE (flight_number);

ALTER TABLE seats
    ADD CONSTRAINT uk_seats_flight_id_seat_number UNIQUE (flight_id, seat_number);

ALTER TABLE bookings
    ADD CONSTRAINT uk_bookings_booking_reference UNIQUE (booking_reference);

CREATE INDEX idx_flights_origin_destination_departure_time
    ON flights (origin, destination, departure_time);

CREATE INDEX idx_seats_flight_id
    ON seats (flight_id);

CREATE INDEX idx_seat_holds_seat_id_expires_at
    ON seat_holds (seat_id, expires_at);

CREATE INDEX idx_bookings_flight_id
    ON bookings (flight_id);

CREATE INDEX idx_bookings_seat_id
    ON bookings (seat_id);
