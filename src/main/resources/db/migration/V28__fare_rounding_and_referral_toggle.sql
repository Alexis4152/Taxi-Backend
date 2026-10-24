ALTER TABLE platform_settings ADD COLUMN fare_round_to_multiple_of_5 BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE platform_settings ADD COLUMN referral_program_enabled BOOLEAN NOT NULL DEFAULT TRUE;
