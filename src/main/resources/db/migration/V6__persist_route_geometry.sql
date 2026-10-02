-- La geometria de la ruta se calculaba de nuevo (llamando al servicio de rutas externo) cada vez
-- que se consultaba el estatus de un viaje, incluso solo para revisar "sigo buscando operador?".
-- Eso hacia que consultar un viaje activo dependiera de un servicio externo y pudiera tardar
-- varios segundos (o mas) si el cache en memoria no estaba caliente (ej. el backend se reinicio).
-- Ahora se calcula una sola vez, al crear el viaje, y se guarda aqui.
ALTER TABLE trips ADD COLUMN route_geometry TEXT;
