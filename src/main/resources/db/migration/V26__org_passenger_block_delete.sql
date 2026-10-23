ALTER TABLE organizations ADD COLUMN deactivation_reason VARCHAR(300);
ALTER TABLE organizations ADD COLUMN pending_deletion BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE passenger_profiles ADD COLUMN deactivation_reason VARCHAR(300);
ALTER TABLE passenger_profiles ADD COLUMN pending_deletion BOOLEAN NOT NULL DEFAULT FALSE;
