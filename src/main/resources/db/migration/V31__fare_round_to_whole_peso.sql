-- Redondeo de centavos al peso mas cercano (>= .50 sube, < .50 baja). Apagado por default para no
-- alterar tarifas existentes hasta que el superadmin lo active (ver PlatformSettings.fareRoundToWholePeso).
ALTER TABLE platform_settings ADD COLUMN fare_round_to_whole_peso BOOLEAN NOT NULL DEFAULT FALSE;
