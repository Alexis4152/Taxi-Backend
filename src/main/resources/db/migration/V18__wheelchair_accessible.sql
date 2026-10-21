ALTER TABLE taxis ADD COLUMN wheelchair_accessible BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE trips ADD COLUMN requires_wheelchair_accessible BOOLEAN NOT NULL DEFAULT false;
