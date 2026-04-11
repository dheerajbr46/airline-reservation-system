CREATE TABLE seats (
    id UUID PRIMARY KEY,
    flight_id UUID NOT NULL,
    seat_number VARCHAR(10) NOT NULL,
    seat_class VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_seats_flight
        FOREIGN KEY (flight_id)
        REFERENCES flights (id)
        ON DELETE CASCADE
);
