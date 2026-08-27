-- Whether an offer holds its evaluators to the hour booked for an interview: a
-- technical exam or a final interview cannot be recorded before it has taken
-- place. Offers already running keep the behaviour they were configured under,
-- so nothing mid funnel locks; new offers wait.

ALTER TABLE offre ADD COLUMN attendre_rendez_vous boolean;

UPDATE offre SET attendre_rendez_vous = false;

ALTER TABLE offre ALTER COLUMN attendre_rendez_vous SET NOT NULL;

ALTER TABLE offre ALTER COLUMN attendre_rendez_vous SET DEFAULT true;
