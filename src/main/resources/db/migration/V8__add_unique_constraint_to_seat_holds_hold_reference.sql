ALTER TABLE seat_holds
    ADD CONSTRAINT uk_seat_holds_hold_reference UNIQUE (hold_reference);
