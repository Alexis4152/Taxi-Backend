-- Rediseno de turnos: en vez de un flag "active" que el admin debe mantener a mano, los turnos
-- se activan/desactivan solos segun su ventana de tiempo (turno "fijo" = sin fecha de fin,
-- vigente hasta que se revoque; turno "temporal" = vigente solo entre start_at y end_at).
-- "revoked" es la unica bandera manual: para terminar un turno antes de tiempo (o uno fijo).

-- Se quitan primero los indices viejos: exigian "a lo mas 1 fila activa por taxi", invariante que
-- no sobrevive al volteo de valores de abajo si ya existen varios turnos historicos por taxi.
DROP INDEX IF EXISTS uq_shifts_active_taxi;
DROP INDEX IF EXISTS idx_shifts_driver_active;

ALTER TABLE shifts RENAME COLUMN active TO revoked;
UPDATE shifts SET revoked = NOT revoked;
ALTER TABLE shifts ALTER COLUMN revoked SET DEFAULT FALSE;

CREATE INDEX idx_shifts_taxi_revoked ON shifts(taxi_id) WHERE revoked = FALSE;
CREATE INDEX idx_shifts_driver_revoked ON shifts(driver_id) WHERE revoked = FALSE;

-- Foto de perfil del pasajero (ademas de la que ya tenia el operador).
ALTER TABLE passenger_profiles ADD COLUMN photo_url VARCHAR(300);

-- Logo de la organizacion, para mostrar su marca a sus propios operadores/administradores.
ALTER TABLE organizations ADD COLUMN logo_url VARCHAR(300);
