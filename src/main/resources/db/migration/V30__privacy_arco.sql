-- Consentimientos de privacidad (LFPDPPP): aceptacion del aviso de privacidad (separada de los
-- terminos y condiciones) y consentimiento opcional para finalidades secundarias (promociones).
-- Las cuentas existentes quedan con privacy_accepted_at en NULL: se les pide aceptar el aviso la
-- proxima vez que entren (ver PrivacyConsentGate en el frontend).
ALTER TABLE users ADD COLUMN privacy_accepted_at TIMESTAMP;
ALTER TABLE users ADD COLUMN marketing_opt_in BOOLEAN NOT NULL DEFAULT FALSE;

-- Cuando se aprueba la eliminacion de una cuenta no se borra la fila (los viajes se conservan por
-- obligaciones fiscales/legales), sino que se anonimiza: se reemplazan nombre, telefono, correo,
-- ubicaciones, etc. Esta marca indica que la cuenta ya fue anonimizada (ver AccountAnonymizationService).
ALTER TABLE users ADD COLUMN anonymized_at TIMESTAMP;

-- Solicitudes de derechos ARCO (Acceso, Rectificacion, Cancelacion, Oposicion) y relacionados
-- (revocacion del consentimiento, limitacion de uso). Plazo legal de respuesta: 20 dias habiles.
CREATE TABLE arco_requests (
    id BIGSERIAL PRIMARY KEY,
    folio VARCHAR(20) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    type VARCHAR(20) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    status VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    response VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    due_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP,
    resolved_by_user_id BIGINT REFERENCES users(id)
);

CREATE INDEX idx_arco_requests_user ON arco_requests(user_id);
CREATE INDEX idx_arco_requests_status ON arco_requests(status);
