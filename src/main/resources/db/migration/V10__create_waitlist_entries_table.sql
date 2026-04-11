CREATE TABLE waitlist_entries (
    id UUID PRIMARY KEY,
    flight_id UUID NOT NULL,
    passenger_name VARCHAR(100) NOT NULL,
    passenger_email VARCHAR(150) NOT NULL,
    preferred_cabin_class VARCHAR(20),
    status VARCHAR(20) NOT NULL,
    queue_position INT NOT NULL,
    promoted_hold_id UUID,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_waitlist_entries_flight
        FOREIGN KEY (flight_id)
        REFERENCES flights (id)
);

CREATE INDEX idx_waitlist_entries_flight_id
    ON waitlist_entries (flight_id);

CREATE INDEX idx_waitlist_entries_status
    ON waitlist_entries (status);

CREATE UNIQUE INDEX uq_waitlist_entries_active_passenger_flight
    ON waitlist_entries (flight_id, lower(passenger_email))
    WHERE status = 'ACTIVE';
