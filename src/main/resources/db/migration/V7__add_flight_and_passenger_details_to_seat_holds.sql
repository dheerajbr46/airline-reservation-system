ALTER TABLE seat_holds
    ADD COLUMN flight_id UUID,
    ADD COLUMN passenger_name VARCHAR(100),
    ADD COLUMN passenger_email VARCHAR(255);

UPDATE seat_holds sh
SET flight_id = s.flight_id
FROM seats s
WHERE sh.seat_id = s.id;

ALTER TABLE seat_holds
    ALTER COLUMN flight_id SET NOT NULL,
    ALTER COLUMN passenger_name SET NOT NULL,
    ALTER COLUMN passenger_email SET NOT NULL;

ALTER TABLE seat_holds
    ADD CONSTRAINT fk_seat_holds_flight
        FOREIGN KEY (flight_id)
        REFERENCES flights (id)
        ON DELETE CASCADE;

CREATE INDEX idx_seat_holds_flight_id
    ON seat_holds (flight_id);
