CREATE TABLE saved_places (
    id BIGSERIAL PRIMARY KEY,
    passenger_user_id BIGINT NOT NULL REFERENCES users(id),
    label VARCHAR(40) NOT NULL,
    address VARCHAR(255) NOT NULL,
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_saved_places_passenger ON saved_places(passenger_user_id);
