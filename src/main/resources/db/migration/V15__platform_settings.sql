-- Fila unica (singleton) con ajustes de la plataforma que antes eran fijos por variable de
-- entorno y ahora el superadmin puede cambiar sin redeploy (ver PlatformSettingsController).
CREATE TABLE platform_settings (
    id BIGINT PRIMARY KEY DEFAULT 1,
    trip_geofence_radius_meters INTEGER NOT NULL DEFAULT 100,
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT platform_settings_singleton CHECK (id = 1)
);

INSERT INTO platform_settings (id, trip_geofence_radius_meters) VALUES (1, 100);
