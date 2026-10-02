-- Chat de texto libre entre pasajero y operador durante un viaje (disponible desde que se acepta
-- hasta que termina). Se guarda el historial completo del viaje.
CREATE TABLE trip_messages (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips(id),
    sender_user_id BIGINT NOT NULL REFERENCES users(id),
    body VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_trip_messages_trip ON trip_messages(trip_id);

-- Viajes programados: hora futura que pide el pasajero (NULL = viaje inmediato, como hasta ahora).
ALTER TABLE trips ADD COLUMN scheduled_at TIMESTAMP;
