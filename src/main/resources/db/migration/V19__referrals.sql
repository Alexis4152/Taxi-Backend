ALTER TABLE users ADD COLUMN referral_code VARCHAR(10);
ALTER TABLE users ADD COLUMN referred_by_user_id BIGINT REFERENCES users(id);

-- Codigo retroactivo para cuentas que ya existian antes de este cambio, para que todo usuario
-- tenga uno (los nuevos lo reciben al registrarse, ver AuthService).
UPDATE users SET referral_code = upper(substr(md5(id::text || random()::text), 1, 6)) WHERE referral_code IS NULL;

ALTER TABLE users ALTER COLUMN referral_code SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT uq_users_referral_code UNIQUE (referral_code);

CREATE INDEX idx_users_referred_by ON users(referred_by_user_id);

ALTER TABLE platform_settings ADD COLUMN referral_reward_description VARCHAR(300);
