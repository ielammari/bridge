-- What each actor can configure.
--
-- A preference row exists only once someone silences a notification: absence
-- means the notification is delivered, so no backfill is needed for the
-- accounts that already exist.

CREATE TABLE preference_notification (
    id_utilisateur    INT         NOT NULL,
    type_notification VARCHAR(30) NOT NULL,
    PRIMARY KEY (id_utilisateur, type_notification),
    CONSTRAINT fk_preference_utilisateur FOREIGN KEY (id_utilisateur)
        REFERENCES utilisateur (id_utilisateur) ON DELETE CASCADE,
    CONSTRAINT ck_preference_type CHECK (type_notification IN
        ('APPLICATION_RECEIVED', 'SCHEDULE_NEEDED', 'INTERVIEW_SCHEDULED'))
);

-- Settings that belong to the company rather than to a person. One row, held
-- at id 1 by the check, so reading it never has to handle an empty table.
CREATE TABLE parametre_organisation (
    id                 SMALLINT PRIMARY KEY,
    premiere_heure     SMALLINT NOT NULL,
    derniere_heure     SMALLINT NOT NULL,
    CONSTRAINT ck_parametre_singleton CHECK (id = 1),
    CONSTRAINT ck_parametre_heures CHECK (
        premiere_heure >= 0 AND derniere_heure <= 23 AND premiere_heure < derniere_heure)
);

INSERT INTO parametre_organisation (id, premiere_heure, derniere_heure) VALUES (1, 9, 16);
