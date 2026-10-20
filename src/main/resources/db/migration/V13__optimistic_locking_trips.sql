-- Bloqueo optimista para evitar que dos operadores acepten el mismo viaje a la vez (ver
-- TripService.acceptOffer): si dos transacciones leen la misma fila y ambas intentan guardar,
-- solo la primera tiene exito; la segunda falla en vez de pisar silenciosamente a la primera.
ALTER TABLE trips ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE trip_offers ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
