-- Personal details collected at signup. They sit on utilisateur because they
-- describe the person, not the job seeking role.
--
-- All four are nullable: accounts created before this migration have none. The
-- birth date is required of new signups, which AuthService enforces.

ALTER TABLE utilisateur ADD COLUMN date_naissance DATE;
ALTER TABLE utilisateur ADD COLUMN sexe           VARCHAR(20);
ALTER TABLE utilisateur ADD COLUMN ville          VARCHAR(100);
ALTER TABLE utilisateur ADD COLUMN pays           VARCHAR(100);

ALTER TABLE utilisateur ADD CONSTRAINT ck_utilisateur_sexe
    CHECK (sexe IS NULL OR sexe IN ('HOMME', 'FEMME', 'AUTRE'));

-- A birth date in the future is a typo, not a person.
ALTER TABLE utilisateur ADD CONSTRAINT ck_utilisateur_date_naissance
    CHECK (date_naissance IS NULL OR date_naissance < CURRENT_DATE);
