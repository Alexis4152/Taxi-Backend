CREATE TABLE trusted_contacts (
    id BIGSERIAL PRIMARY KEY,
    passenger_user_id BIGINT NOT NULL REFERENCES users(id),
    name VARCHAR(80) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_trusted_contacts_passenger ON trusted_contacts(passenger_user_id);
