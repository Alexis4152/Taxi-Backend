-- Plantillas de turno (ej. "Turno Mañana" 06:00-18:00, "Turno Noche" 18:00-06:00) que el admin
-- da de alta/edita/elimina por organizacion. Un turno FIJO debe referenciar una de estas
-- plantillas (define su horario diario); un turno TEMPORAL sigue usando start_at/end_at libres.
CREATE TABLE shift_templates (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id),
    name VARCHAR(60) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(organization_id, name)
);

ALTER TABLE shifts ADD COLUMN shift_template_id BIGINT REFERENCES shift_templates(id);

-- Quien cancelo el viaje (para las estadisticas del operador: "cuantos le cancelo el pasajero").
ALTER TABLE trips ADD COLUMN cancelled_by_role VARCHAR(10);
