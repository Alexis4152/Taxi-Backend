-- Antes fijo por variable de entorno (DISPATCH_RADIUS_KM), ahora configurable en caliente por el
-- superadmin igual que trip_geofence_radius_meters (ver PlatformSettingsController). Controla:
-- (1) a cuantos km un operador recibe ofertas de viajes nuevos, y (2) a cuantos km un pasajero ve
-- taxis cercanos en el mapa antes de pedir un viaje - es el mismo radio para ambos casos.
ALTER TABLE platform_settings ADD COLUMN dispatch_search_radius_km NUMERIC(5,2) NOT NULL DEFAULT 6;
