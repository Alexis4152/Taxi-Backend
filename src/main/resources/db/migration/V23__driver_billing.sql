-- Clave interbancaria (CLABE) a la que los operadores transfieren lo que deben a la plataforma, y
-- el cobro extra (fijo o %) que se le suma al pasajero y se le carga al operador por cada viaje.
ALTER TABLE platform_settings ADD COLUMN clabe VARCHAR(18);
ALTER TABLE platform_settings ADD COLUMN operator_fee_type VARCHAR(10) NOT NULL DEFAULT 'FIXED';
ALTER TABLE platform_settings ADD COLUMN operator_fee_value NUMERIC(8,2) NOT NULL DEFAULT 0;
ALTER TABLE platform_settings ADD COLUMN passenger_fee_type VARCHAR(10) NOT NULL DEFAULT 'FIXED';
ALTER TABLE platform_settings ADD COLUMN passenger_fee_value NUMERIC(8,2) NOT NULL DEFAULT 0;
-- Hora del dia siguiente (0-23) antes de la cual hay que pagar, y el maximo de viajes sin pagar
-- antes de bloquearse, lo que llegue primero.
ALTER TABLE platform_settings ADD COLUMN payment_deadline_hour INTEGER NOT NULL DEFAULT 12;
ALTER TABLE platform_settings ADD COLUMN payment_trip_threshold INTEGER NOT NULL DEFAULT 20;

-- Cobro de cada viaje (fijo o % segun la configuracion vigente al momento), guardado en el viaje
-- para no depender de que la configuracion no cambie despues y poder auditar cuanto se cobro.
ALTER TABLE trips ADD COLUMN passenger_service_fee NUMERIC(8,2) NOT NULL DEFAULT 0;
ALTER TABLE trips ADD COLUMN operator_service_fee NUMERIC(8,2) NOT NULL DEFAULT 0;

-- Saldo acumulado que el operador debe transferir a la plataforma (suma de ambos cobros de cada
-- viaje completado), cuantos de esos viajes lleva sin pagar, para cuando vence el plazo, y si ya
-- se le bloqueo el acceso a conectarse por no pagar a tiempo.
ALTER TABLE driver_profiles ADD COLUMN pending_amount NUMERIC(10,2) NOT NULL DEFAULT 0;
ALTER TABLE driver_profiles ADD COLUMN unpaid_trip_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE driver_profiles ADD COLUMN payment_due_at TIMESTAMP;
ALTER TABLE driver_profiles ADD COLUMN payment_blocked BOOLEAN NOT NULL DEFAULT false;
