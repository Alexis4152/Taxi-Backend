ALTER TABLE organizations ADD COLUMN is_independent_pool BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE driver_profiles ADD COLUMN taxi_id BIGINT REFERENCES taxis(id);

CREATE TABLE taxi_change_requests (
    id BIGSERIAL PRIMARY KEY,
    driver_id BIGINT NOT NULL REFERENCES driver_profiles(id),
    requested_unit_number VARCHAR(20),
    requested_plates VARCHAR(15) NOT NULL,
    requested_brand VARCHAR(60),
    requested_model VARCHAR(60),
    reason VARCHAR(300),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    resolved_at TIMESTAMP,
    resolved_by_user_id BIGINT REFERENCES users(id)
);
