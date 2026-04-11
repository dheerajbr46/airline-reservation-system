CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    flight_id UUID NOT NULL,
    seat_id UUID NOT NULL,
    seat_hold_id UUID NOT NULL,
    booking_reference VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    booked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_bookings_flight
        FOREIGN KEY (flight_id)
        REFERENCES flights (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_bookings_seat
        FOREIGN KEY (seat_id)
        REFERENCES seats (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_bookings_seat_hold
        FOREIGN KEY (seat_hold_id)
        REFERENCES seat_holds (id)
        ON DELETE RESTRICT
);
