-- Esquema inicial de TaxiApp (BITFX)

CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    contact_phone VARCHAR(20),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(20) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150),
    role VARCHAR(20) NOT NULL CHECK (role IN ('SUPER_ADMIN','ADMIN','DRIVER','PASSENGER')),
    organization_id BIGINT REFERENCES organizations(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE driver_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    photo_url VARCHAR(300),
    bank_account VARCHAR(50),
    rating_avg NUMERIC(3,2) NOT NULL DEFAULT 0,
    rating_count INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE passenger_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    rating_avg NUMERIC(3,2) NOT NULL DEFAULT 0,
    rating_count INT NOT NULL DEFAULT 0
);

CREATE TABLE taxis (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    unit_number VARCHAR(20) NOT NULL,
    plates VARCHAR(15) NOT NULL UNIQUE,
    photo_url VARCHAR(300),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(organization_id, unit_number)
);

CREATE TABLE shifts (
    id BIGSERIAL PRIMARY KEY,
    taxi_id BIGINT NOT NULL REFERENCES taxis(id),
    driver_id BIGINT NOT NULL REFERENCES driver_profiles(id),
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP,
    temporary BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Garantiza a nivel de base de datos que un taxi no tenga mas de un turno activo a la vez
-- (regla de negocio: 1 solo operador por unidad en un momento dado).
CREATE UNIQUE INDEX uq_shifts_active_taxi ON shifts(taxi_id) WHERE active = TRUE;
CREATE INDEX idx_shifts_driver_active ON shifts(driver_id) WHERE active = TRUE;

CREATE TABLE driver_locations (
    id BIGSERIAL PRIMARY KEY,
    driver_id BIGINT NOT NULL UNIQUE REFERENCES driver_profiles(id),
    lat DOUBLE PRECISION NOT NULL,
    lng DOUBLE PRECISION NOT NULL,
    heading DOUBLE PRECISION,
    online BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_driver_locations_online ON driver_locations(online);

CREATE TABLE tariff_rules (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT REFERENCES organizations(id),
    base_fare NUMERIC(10,2) NOT NULL,
    per_km NUMERIC(10,2) NOT NULL,
    min_fare NUMERIC(10,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE NULLS NOT DISTINCT (organization_id)
);

CREATE TABLE trips (
    id BIGSERIAL PRIMARY KEY,
    passenger_id BIGINT NOT NULL REFERENCES users(id),
    organization_id BIGINT REFERENCES organizations(id),
    driver_id BIGINT REFERENCES driver_profiles(id),
    taxi_id BIGINT REFERENCES taxis(id),
    origin_lat DOUBLE PRECISION NOT NULL,
    origin_lng DOUBLE PRECISION NOT NULL,
    origin_address VARCHAR(300) NOT NULL,
    destination_lat DOUBLE PRECISION NOT NULL,
    destination_lng DOUBLE PRECISION NOT NULL,
    destination_address VARCHAR(300) NOT NULL,
    status VARCHAR(25) NOT NULL,
    distance_km NUMERIC(8,2),
    duration_min NUMERIC(8,2),
    estimated_fare NUMERIC(10,2),
    payment_method VARCHAR(15) NOT NULL,
    payment_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    requested_at TIMESTAMP NOT NULL DEFAULT now(),
    accepted_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancel_reason VARCHAR(300)
);

CREATE INDEX idx_trips_status ON trips(status);
CREATE INDEX idx_trips_passenger ON trips(passenger_id);
CREATE INDEX idx_trips_driver ON trips(driver_id);

CREATE TABLE trip_offers (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips(id),
    driver_id BIGINT NOT NULL REFERENCES driver_profiles(id),
    status VARCHAR(15) NOT NULL DEFAULT 'SENT',
    sent_at TIMESTAMP NOT NULL DEFAULT now(),
    expires_at TIMESTAMP NOT NULL,
    responded_at TIMESTAMP,
    UNIQUE(trip_id, driver_id)
);

CREATE INDEX idx_trip_offers_driver_status ON trip_offers(driver_id, status);

CREATE TABLE ratings (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips(id),
    from_user_id BIGINT NOT NULL REFERENCES users(id),
    to_user_id BIGINT NOT NULL REFERENCES users(id),
    direction VARCHAR(20) NOT NULL CHECK (direction IN ('PASSENGER_TO_DRIVER','DRIVER_TO_PASSENGER')),
    score SMALLINT NOT NULL CHECK (score BETWEEN 1 AND 5),
    comment VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(trip_id, direction)
);

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    token VARCHAR(200) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    token VARCHAR(200) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
