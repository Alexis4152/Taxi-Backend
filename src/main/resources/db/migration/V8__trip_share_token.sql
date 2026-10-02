ALTER TABLE trips ADD COLUMN share_token VARCHAR(40);
CREATE UNIQUE INDEX idx_trips_share_token ON trips (share_token);
