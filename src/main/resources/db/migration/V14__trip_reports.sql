CREATE TABLE trip_reports (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips(id),
    reporter_user_id BIGINT NOT NULL REFERENCES users(id),
    category VARCHAR(20) NOT NULL,
    description VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    resolved_at TIMESTAMP,
    resolved_by_user_id BIGINT REFERENCES users(id)
);

CREATE INDEX idx_trip_reports_status ON trip_reports(status);
CREATE INDEX idx_trip_reports_reporter ON trip_reports(reporter_user_id);
