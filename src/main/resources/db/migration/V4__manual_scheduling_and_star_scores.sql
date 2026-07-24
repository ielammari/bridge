-- Interviews are scheduled manually by HR instead of from evaluator declared
-- availability, and the technical score becomes a half star scale.
--
-- This is a deliberate departure from the original relational model: the
-- Creneau (availability) concept is removed, an appointment carries its own
-- date and time, and there is a single company wide calendar (one interview
-- per date and time).

-- ---------------------------------------------------------------------------
-- 1. An appointment carries its own date and time.
-- ---------------------------------------------------------------------------

-- Dropping the column also drops its foreign key and unique index.
ALTER TABLE rendez_vous DROP COLUMN id_creneau;

ALTER TABLE rendez_vous ADD COLUMN date_rendez_vous  DATE;
ALTER TABLE rendez_vous ADD COLUMN heure_rendez_vous TIME;

-- The table is empty at this point in every environment; the backfill only
-- guards the NOT NULL tightening.
UPDATE rendez_vous SET date_rendez_vous = CURRENT_DATE, heure_rendez_vous = TIME '09:00'
WHERE date_rendez_vous IS NULL;

ALTER TABLE rendez_vous ALTER COLUMN date_rendez_vous  SET NOT NULL;
ALTER TABLE rendez_vous ALTER COLUMN heure_rendez_vous SET NOT NULL;

-- One interview per slot across the whole company.
ALTER TABLE rendez_vous ADD CONSTRAINT uq_rdv_date_heure UNIQUE (date_rendez_vous, heure_rendez_vous);

-- ---------------------------------------------------------------------------
-- 2. Availability declaration is gone.
-- ---------------------------------------------------------------------------

DROP TABLE creneau_disponibilite;

-- ---------------------------------------------------------------------------
-- 3. Half star scoring, stored as 0 to 10 half star units.
-- ---------------------------------------------------------------------------

ALTER TABLE noter DROP CONSTRAINT ck_noter_note;
ALTER TABLE noter ADD CONSTRAINT ck_noter_note CHECK (note BETWEEN 0 AND 10);
