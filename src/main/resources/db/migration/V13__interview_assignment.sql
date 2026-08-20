-- An interview names who runs it: the expert HR chose for a technical exam, and
-- the recruiter who published the offer for an HR interview.
ALTER TABLE rendez_vous ADD COLUMN id_evaluateur integer;

-- What the funnel already recorded says who ran it.
UPDATE rendez_vous r
SET id_evaluateur = e.id_evaluateur
FROM evaluation e
WHERE e.id_rendez_vous = r.id_rendez_vous;

UPDATE rendez_vous r
SET id_evaluateur = o.id_rh
FROM candidature c
JOIN offre o ON o.id_offre = c.id_offre
WHERE c.id_candidature = r.id_candidature
  AND r.id_evaluateur IS NULL
  AND r.type = 'RH';

-- An exam nobody sat yet falls to the first expert until HR reassigns it.
UPDATE rendez_vous r
SET id_evaluateur = (SELECT min(id_expert) FROM expert_technique)
WHERE r.id_evaluateur IS NULL
  AND r.type = 'TECHNIQUE';

ALTER TABLE rendez_vous ALTER COLUMN id_evaluateur SET NOT NULL;

ALTER TABLE rendez_vous
    ADD CONSTRAINT fk_rdv_evaluateur FOREIGN KEY (id_evaluateur)
    REFERENCES evaluateur (id_evaluateur);

-- The calendar belongs to the evaluator rather than to the company: two of them
-- may hold an interview at the same hour, neither may hold two.
ALTER TABLE rendez_vous DROP CONSTRAINT uq_rdv_date_heure;

ALTER TABLE rendez_vous
    ADD CONSTRAINT uq_rdv_evaluateur_creneau
    UNIQUE (id_evaluateur, date_rendez_vous, heure_rendez_vous);

-- A scheduled exam is a work order addressed to one expert, so it is delivered
-- whatever their preferences say.
DELETE FROM preference_notification WHERE type_notification = 'INTERVIEW_SCHEDULED';

ALTER TABLE preference_notification DROP CONSTRAINT ck_preference_type;

ALTER TABLE preference_notification
    ADD CONSTRAINT ck_preference_type CHECK (type_notification IN (
        'APPLICATION_RECEIVED',
        'APPLICATION_SUBMITTED',
        'SCHEDULE_NEEDED',
        'EXAM_OVERDUE'));
