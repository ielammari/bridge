-- The demonstration set: two recruiters, three technical experts, thirty
-- candidates, twenty four offers, and forty two applications at every stage of
-- the funnel. Every content table is emptied first and its identifiers restart
-- at 1; the trait catalogue and the interview window are kept.
--
-- Every account uses the password Bridge123!. Dates are relative to the day the
-- script runs, and the six offers published last are the ones the landing page
-- shows. Each funnel act writes the rows and the notification its service
-- writes, in the same words. DemoData reruns the script on BRIDGE_DEMO_RESET.
--
-- CV paths follow {candidateId}/demo-{name}.pdf, each a copy of demo/cv.pdf.


-- The list is explicit: a table missing from it stops the script instead of
-- being emptied by cascade.

TRUNCATE utilisateur, candidat, evaluateur, responsable_rh, expert_technique, cv, formation,
    posseder, offre, exiger, offre_enregistree, candidature, rendez_vous, evaluation, noter,
    entretien_rh, embauche, message, preference_notification
    RESTART IDENTITY;


-- Helpers, scoped to this session.

CREATE FUNCTION pg_temp.password() RETURNS TEXT LANGUAGE sql IMMUTABLE AS $$
    SELECT '{bcrypt}$2a$10$5gpc0XV73c8DM..7l88o5O6K79B7LkACA7QLlIZT.Iz.Ln4GrDuM6'
$$;

CREATE FUNCTION pg_temp.at(shift INT, hour INT) RETURNS TIMESTAMP LANGUAGE sql AS $$
    SELECT (CURRENT_DATE + shift) + make_time(hour, 0, 0)
$$;

-- A weekday, stepping off a weekend in the direction of the shift.
CREATE FUNCTION pg_temp.workday(shift INT) RETURNS DATE LANGUAGE plpgsql AS $$
DECLARE
    d DATE := CURRENT_DATE + shift;
BEGIN
    WHILE EXTRACT(ISODOW FROM d) > 5 LOOP
        d := d + CASE WHEN shift < 0 THEN -1 ELSE 1 END;
    END LOOP;
    RETURN d;
END
$$;

-- The date as NotificationService writes it: "3 octobre 2026 à 09h00".
CREATE FUNCTION pg_temp.spoken(d DATE, hour INT) RETURNS TEXT LANGUAGE sql AS $$
    SELECT EXTRACT(DAY FROM d)::INT || ' '
        || (ARRAY['janvier', 'février', 'mars', 'avril', 'mai', 'juin', 'juillet',
                  'août', 'septembre', 'octobre', 'novembre', 'décembre'])[EXTRACT(MONTH FROM d)::INT]
        || ' ' || EXTRACT(YEAR FROM d)::INT || ' à ' || lpad(hour::TEXT, 2, '0') || 'h00'
$$;

-- One entry of a list, picked by a stable hash of the key.
CREATE FUNCTION pg_temp.pick(p_key TEXT, p_choices TEXT[]) RETURNS TEXT LANGUAGE sql IMMUTABLE AS $$
    SELECT p_choices[1 + abs(hashtext(p_key)) % array_length(p_choices, 1)]
$$;

CREATE FUNCTION pg_temp.account(p_email TEXT) RETURNS INT LANGUAGE plpgsql AS $$
DECLARE
    found INT;
BEGIN
    SELECT id_utilisateur INTO found FROM utilisateur WHERE email = p_email;
    IF found IS NULL THEN
        RAISE EXCEPTION 'No account %', p_email;
    END IF;
    RETURN found;
END
$$;

CREATE FUNCTION pg_temp.trait(p_label TEXT) RETURNS INT LANGUAGE plpgsql AS $$
DECLARE
    found INT;
BEGIN
    SELECT id_trait INTO STRICT found FROM trait WHERE libelle = p_label;
    RETURN found;
EXCEPTION
    WHEN NO_DATA_FOUND OR TOO_MANY_ROWS THEN
        RAISE EXCEPTION 'Trait % is not exactly one row', p_label;
END
$$;

CREATE FUNCTION pg_temp.offer_id(p_title TEXT) RETURNS INT LANGUAGE plpgsql AS $$
DECLARE
    found INT;
BEGIN
    SELECT id_offre INTO found FROM offre WHERE titre = p_title;
    IF found IS NULL THEN
        RAISE EXCEPTION 'No offer %', p_title;
    END IF;
    RETURN found;
END
$$;

CREATE FUNCTION pg_temp.name_of(p_candidate INT) RETURNS TEXT LANGUAGE sql AS $$
    SELECT prenom || ' ' || nom FROM utilisateur WHERE id_utilisateur = p_candidate
$$;

-- A system notification, already read once it is a few days old.
CREATE FUNCTION pg_temp.notice(p_to INT, p_application INT, p_type TEXT, p_content TEXT,
        p_sent TIMESTAMP) RETURNS VOID LANGUAGE sql AS $$
    INSERT INTO message (contenu, date_envoi, lu, id_expediteur, id_destinataire, id_candidature,
                         type_notification)
    VALUES (p_content, p_sent, p_sent < now() - INTERVAL '3 days', NULL, p_to, p_application, p_type)
$$;


-- People.

CREATE FUNCTION pg_temp.staff(p_email TEXT, p_first TEXT, p_last TEXT, p_role TEXT, p_detail TEXT,
        p_since INT, p_country TEXT) RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    id INT;
BEGIN
    INSERT INTO utilisateur (email, mot_de_passe, nom, prenom, date_inscription, role, pays)
    VALUES (p_email, pg_temp.password(), p_last, p_first, CURRENT_DATE + p_since, p_role, p_country)
    RETURNING id_utilisateur INTO id;
    INSERT INTO evaluateur (id_evaluateur) VALUES (id);
    IF p_role = 'RH' THEN
        INSERT INTO responsable_rh (id_rh, departement) VALUES (id, p_detail);
    ELSE
        INSERT INTO expert_technique (id_expert, specialite) VALUES (id, p_detail);
    END IF;
END
$$;

CREATE FUNCTION pg_temp.candidate(p_email TEXT, p_first TEXT, p_last TEXT, p_gender TEXT,
        p_birth DATE, p_phone TEXT, p_city TEXT, p_degree TEXT, p_since INT, p_traits TEXT[])
        RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    id INT;
    cv TEXT;
BEGIN
    INSERT INTO utilisateur (email, mot_de_passe, nom, prenom, telephone, date_inscription, role,
                             date_naissance, sexe, ville, pays)
    VALUES (p_email, pg_temp.password(), p_last, p_first, p_phone, CURRENT_DATE + p_since,
            'CANDIDAT', p_birth, p_gender, p_city, 'France')
    RETURNING id_utilisateur INTO id;

    cv := id || '/demo-' || replace(split_part(p_email, '@', 1), '.', '-') || '.pdf';
    INSERT INTO candidat (id_candidat, chemin_cv, diplome) VALUES (id, cv, p_degree);
    INSERT INTO cv (id_candidat, intitule, chemin, date_depot)
    VALUES (id, 'CV ' || p_first || ' ' || p_last, cv, pg_temp.at(p_since, 18));
    INSERT INTO posseder (id_candidat, id_trait)
    SELECT id, pg_temp.trait(label) FROM unnest(p_traits) AS label;
END
$$;

CREATE FUNCTION pg_temp.education(p_email TEXT, p_title TEXT, p_school TEXT, p_field TEXT,
        p_from INT, p_to INT) RETURNS VOID LANGUAGE sql AS $$
    INSERT INTO formation (id_candidat, intitule, etablissement, domaine, annee_debut, annee_fin)
    VALUES (pg_temp.account(p_email), p_title, p_school, p_field, p_from, p_to)
$$;


-- Offers.

CREATE FUNCTION pg_temp.offer(p_hr TEXT, p_title TEXT, p_company TEXT, p_degree TEXT,
        p_contract TEXT, p_location TEXT, p_remote TEXT, p_min NUMERIC, p_max NUMERIC,
        p_published INT, p_status TEXT, p_wait BOOLEAN, p_required TEXT[], p_plus TEXT[],
        p_description TEXT) RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    id INT;
BEGIN
    INSERT INTO offre (titre, entreprise, description, diplome_requis, type_contrat, localisation,
                       modalite_teletravail, salaire_min, salaire_max, date_publication, statut,
                       id_rh, attendre_rendez_vous)
    VALUES (p_title, COALESCE(p_company, 'Bridge'), p_description, p_degree, p_contract, p_location,
            p_remote, p_min, p_max, CURRENT_DATE + p_published, p_status, pg_temp.account(p_hr),
            p_wait)
    RETURNING id_offre INTO id;
    INSERT INTO exiger (id_offre, id_trait, est_obligatoire)
    SELECT id, pg_temp.trait(label), TRUE FROM unnest(p_required) AS label;
    INSERT INTO exiger (id_offre, id_trait, est_obligatoire)
    SELECT id, pg_temp.trait(label), FALSE FROM unnest(p_plus) AS label;
END
$$;


-- The funnel, one function per act.

-- ApplicationService.apply, refusing a pair the matching gate would refuse.
CREATE FUNCTION pg_temp.apply(p_email TEXT, p_title TEXT, p_shift INT, p_hour INT)
        RETURNS INT LANGUAGE plpgsql AS $$
DECLARE
    ladder CONSTANT TEXT[] := ARRAY['BAC', 'BAC_2', 'BAC_3', 'BAC_5', 'DOCTORAT'];
    who INT := pg_temp.account(p_email);
    what INT := pg_temp.offer_id(p_title);
    hr INT;
    id INT;
    sent TIMESTAMP := pg_temp.at(p_shift, p_hour);
BEGIN
    IF EXISTS (SELECT 1 FROM exiger e
               WHERE e.id_offre = what AND e.est_obligatoire
                 AND NOT EXISTS (SELECT 1 FROM posseder p
                                 WHERE p.id_candidat = who AND p.id_trait = e.id_trait))
       OR (SELECT array_position(ladder, c.diplome) < array_position(ladder, o.diplome_requis)
           FROM candidat c, offre o WHERE c.id_candidat = who AND o.id_offre = what) THEN
        RAISE EXCEPTION '% does not match %', p_email, p_title;
    END IF;

    INSERT INTO candidature (date_candidature, cv_joint, statut, id_candidat, id_offre)
    SELECT sent, chemin_cv, 'NOUVELLE', who, what FROM candidat WHERE id_candidat = who
    RETURNING id_candidature INTO id;

    SELECT id_rh INTO hr FROM offre WHERE id_offre = what;
    PERFORM pg_temp.notice(who, id, 'APPLICATION_SUBMITTED',
        'Votre candidature pour l''offre « ' || p_title || ' » a bien été enregistrée.', sent);
    PERFORM pg_temp.notice(hr, id, 'APPLICATION_RECEIVED',
        'Nouvelle candidature de ' || pg_temp.name_of(who) || ' pour l''offre « ' || p_title || ' ».',
        sent);
    RETURN id;
END
$$;

-- EvaluationService.review: the recruiter has opened it.
CREATE FUNCTION pg_temp.review(p_application INT) RETURNS VOID LANGUAGE sql AS $$
    UPDATE candidature SET statut = 'EN_REVUE' WHERE id_candidature = p_application
$$;

-- A refusal at any stage, told to the candidate.
CREATE FUNCTION pg_temp.refuse(p_application INT, p_sent TIMESTAMP) RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    who INT;
    title TEXT;
BEGIN
    UPDATE candidature SET statut = 'REFUSEE' WHERE id_candidature = p_application
    RETURNING id_candidat INTO who;
    SELECT o.titre INTO title FROM candidature c JOIN offre o USING (id_offre)
    WHERE c.id_candidature = p_application;
    PERFORM pg_temp.notice(who, p_application, 'REJECTED',
        'Votre candidature pour l''offre « ' || title || ' » n''a pas été retenue.', p_sent);
END
$$;

-- An approval that advances the application and asks the recruiter to schedule.
CREATE FUNCTION pg_temp.advance(p_application INT, p_to TEXT, p_sent TIMESTAMP)
        RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    hr INT;
    who INT;
    title TEXT;
BEGIN
    UPDATE candidature SET statut = p_to WHERE id_candidature = p_application;
    SELECT o.id_rh, c.id_candidat, o.titre INTO hr, who, title
    FROM candidature c JOIN offre o USING (id_offre) WHERE c.id_candidature = p_application;
    PERFORM pg_temp.notice(hr, p_application, 'SCHEDULE_NEEDED',
        'Planifiez ' || CASE p_to WHEN 'EXAMEN_TECHNIQUE' THEN 'l''examen technique' ELSE 'l''entretien RH' END
            || ' pour ' || pg_temp.name_of(who) || ' (offre « ' || title || ' »).',
        p_sent);
END
$$;

-- EvaluationService.preselect.
CREATE FUNCTION pg_temp.preselect(p_application INT, p_decision TEXT, p_comment TEXT, p_shift INT,
        p_hour INT) RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    sent TIMESTAMP := pg_temp.at(p_shift, p_hour);
BEGIN
    INSERT INTO evaluation (type, commentaire, decision, date_evaluation, id_candidature, id_evaluateur)
    SELECT 'PRESELECTION', p_comment, p_decision, sent, p_application, o.id_rh
    FROM candidature c JOIN offre o USING (id_offre) WHERE c.id_candidature = p_application;
    IF p_decision = 'VALIDEE' THEN
        PERFORM pg_temp.advance(p_application, 'EXAMEN_TECHNIQUE', sent);
    ELSE
        PERFORM pg_temp.refuse(p_application, sent);
    END IF;
END
$$;

-- AppointmentService.schedule. The hour asked for, or the first one after it
-- that neither the evaluator nor the candidate already holds. A final
-- interview is run by the offer's recruiter, so it names nobody.
CREATE FUNCTION pg_temp.book(p_application INT, p_type TEXT, p_expert TEXT, p_day INT, p_hour INT,
        p_booked INT) RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    first_hour INT;
    last_hour INT;
    who INT;
    hr INT;
    title TEXT;
    runner INT;
    d DATE := pg_temp.workday(p_day);
    h INT := p_hour;
    sent TIMESTAMP := pg_temp.at(p_booked, 16);
BEGIN
    SELECT premiere_heure, derniere_heure INTO first_hour, last_hour FROM parametre_organisation;
    SELECT c.id_candidat, o.id_rh, o.titre INTO who, hr, title
    FROM candidature c JOIN offre o USING (id_offre) WHERE c.id_candidature = p_application;
    runner := CASE WHEN p_type = 'TECHNIQUE' THEN pg_temp.account(p_expert) ELSE hr END;

    WHILE EXISTS (SELECT 1 FROM rendez_vous r JOIN candidature c USING (id_candidature)
                  WHERE r.date_rendez_vous = d AND r.heure_rendez_vous = make_time(h, 0, 0)
                    AND (r.id_evaluateur = runner OR c.id_candidat = who)) LOOP
        h := h + 1;
        IF h > last_hour THEN
            h := first_hour;
            d := d + 1;
            WHILE EXTRACT(ISODOW FROM d) > 5 LOOP
                d := d + 1;
            END LOOP;
        END IF;
    END LOOP;

    INSERT INTO rendez_vous (type, statut, id_candidature, date_rendez_vous, heure_rendez_vous,
                             id_evaluateur)
    VALUES (p_type, 'PLANIFIE', p_application, d, make_time(h, 0, 0), runner);

    PERFORM pg_temp.notice(who, p_application, 'INTERVIEW_SCHEDULED',
        CASE p_type WHEN 'TECHNIQUE' THEN 'Votre examen technique' ELSE 'Votre entretien RH' END
            || ' est fixé au ' || pg_temp.spoken(d, h) || '.',
        sent);
    IF p_type = 'TECHNIQUE' THEN
        PERFORM pg_temp.notice(runner, p_application, 'INTERVIEW_SCHEDULED',
            'Examen technique à faire passer à ' || pg_temp.name_of(who) || ' le '
                || pg_temp.spoken(d, h) || ' (offre « ' || title || ' »).',
            sent);
    END IF;
END
$$;

-- The booked interview of that type, marked as held, refusing one whose hour
-- has not come. Returns its id, who ran it, and the moment its record was
-- written, an hour after it began.
CREATE FUNCTION pg_temp.hold(p_application INT, p_type TEXT, OUT appointment INT,
        OUT runner INT, OUT recorded TIMESTAMP) LANGUAGE plpgsql AS $$
BEGIN
    UPDATE rendez_vous SET statut = 'REALISE'
    WHERE id_candidature = p_application AND type = p_type
    RETURNING id_rendez_vous, id_evaluateur, date_rendez_vous + heure_rendez_vous + INTERVAL '1 hour'
    INTO appointment, runner, recorded;
    IF appointment IS NULL OR recorded > now() THEN
        RAISE EXCEPTION 'Application % has no % interview held yet', p_application, p_type;
    END IF;
END
$$;

-- EvaluationService.evaluateTechnical. Scores are half stars, 0 to 10.
CREATE FUNCTION pg_temp.technical(p_application INT, p_decision TEXT, p_comment TEXT, p_scores JSONB)
        RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    held RECORD;
    id INT;
BEGIN
    SELECT * INTO held FROM pg_temp.hold(p_application, 'TECHNIQUE');
    INSERT INTO evaluation (type, commentaire, decision, date_evaluation, id_candidature,
                            id_evaluateur, id_rendez_vous)
    VALUES ('TECHNIQUE', p_comment, p_decision, held.recorded, p_application, held.runner,
            held.appointment)
    RETURNING id_evaluation INTO id;
    INSERT INTO noter (id_evaluation, id_trait, note)
    SELECT id, pg_temp.trait(score.key), score.value::SMALLINT FROM jsonb_each_text(p_scores) AS score;

    IF EXISTS (SELECT 1 FROM noter n JOIN candidature c ON c.id_candidature = p_application
               WHERE n.id_evaluation = id
                 AND NOT EXISTS (SELECT 1 FROM exiger e
                                 WHERE e.id_offre = c.id_offre AND e.id_trait = n.id_trait))
       OR EXISTS (SELECT 1 FROM exiger e JOIN candidature c ON c.id_offre = e.id_offre
                  WHERE c.id_candidature = p_application AND e.est_obligatoire
                    AND NOT EXISTS (SELECT 1 FROM noter n
                                    WHERE n.id_evaluation = id AND n.id_trait = e.id_trait)) THEN
        RAISE EXCEPTION 'Application % is not scored on exactly its offer''s traits', p_application;
    END IF;

    IF p_decision = 'VALIDEE' THEN
        PERFORM pg_temp.advance(p_application, 'ENTRETIEN_RH', held.recorded);
    ELSE
        PERFORM pg_temp.refuse(p_application, held.recorded);
    END IF;
END
$$;

-- EvaluationService.finalize. The interview data is kept whatever the outcome;
-- the hiring terms exist only for a hire. Day fields are shifts from today.
CREATE FUNCTION pg_temp.final(p_application INT, p_decision TEXT, p_comment TEXT, p_interview JSONB,
        p_hiring JSONB) RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    held RECORD;
    who INT;
    title TEXT;
BEGIN
    SELECT * INTO held FROM pg_temp.hold(p_application, 'RH');
    INSERT INTO evaluation (type, commentaire, decision, date_evaluation, id_candidature,
                            id_evaluateur, id_rendez_vous)
    VALUES ('ENTRETIEN_RH', p_comment, p_decision, held.recorded, p_application, held.runner,
            held.appointment);
    INSERT INTO entretien_rh (salaire_attendu, date_disponibilite, type_contrat_envisage,
                              duree_preavis, flexibilite_horaire, attentes_teletravail,
                              adequation_culture, id_candidature)
    VALUES ((p_interview->>'salary')::NUMERIC, CURRENT_DATE + (p_interview->>'available')::INT,
            p_interview->>'contract', p_interview->>'notice', p_interview->>'flexibility',
            p_interview->>'remote', p_interview->>'culture', p_application);

    IF p_decision = 'VALIDEE' THEN
        INSERT INTO embauche (salaire_negocie, date_prise_poste, type_contrat_final,
                              duree_periode_essai, statut_cadre, avantages, id_candidature)
        VALUES ((p_hiring->>'salary')::NUMERIC, CURRENT_DATE + (p_hiring->>'start')::INT,
                p_hiring->>'contract', p_hiring->>'trial', p_hiring->>'executive',
                p_hiring->>'benefits', p_application);
        UPDATE candidature SET statut = 'EMBAUCHEE' WHERE id_candidature = p_application
        RETURNING id_candidat INTO who;
        SELECT o.titre INTO title FROM candidature c JOIN offre o USING (id_offre)
        WHERE c.id_candidature = p_application;
        PERFORM pg_temp.notice(who, p_application, 'HIRED',
            'Félicitations, vous êtes embauché(e) pour l''offre « ' || title || ' ».', held.recorded);
    ELSE
        PERFORM pg_temp.refuse(p_application, held.recorded);
    END IF;
END
$$;

-- The whole funnel to a named stage, on a fixed rhythm from the day of
-- applying: screening the next day, the exam booked the day after and held
-- three days later, the final interview booked two days after the exam and
-- held three days after that. p_next is the day of the next interview when the
-- stage stops at a booked one. Comments and scores are drawn from a stable
-- hash, so every run of this migration writes the same rows.
CREATE FUNCTION pg_temp.journey(p_email TEXT, p_title TEXT, p_applied INT, p_stage TEXT,
        p_expert TEXT DEFAULT NULL, p_next INT DEFAULT NULL) RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    stages CONSTANT TEXT[] := ARRAY['NOUVELLE', 'EN_REVUE', 'REFUSEE_PRESELECTION',
        'EXAMEN_A_PLANIFIER', 'EXAMEN_PLANIFIE', 'REFUSEE_TECHNIQUE', 'ENTRETIEN_A_PLANIFIER',
        'ENTRETIEN_PLANIFIE', 'REFUSEE_FINALE', 'EMBAUCHEE'];
    key TEXT := p_email || p_title;
    hour INT := 9 + abs(hashtext(key)) % 8;
    exam_day INT := p_applied + 5;
    final_day INT := p_applied + 10;
    passed BOOLEAN;
    o RECORD;
    a INT;
    scores JSONB;
    middle NUMERIC;
BEGIN
    IF NOT p_stage = ANY (stages) THEN
        RAISE EXCEPTION 'Unknown stage %', p_stage;
    END IF;
    SELECT * INTO o FROM offre WHERE id_offre = pg_temp.offer_id(p_title);

    a := pg_temp.apply(p_email, p_title, p_applied, hour);
    IF p_stage = 'NOUVELLE' THEN
        RETURN;
    ELSIF p_stage = 'EN_REVUE' THEN
        PERFORM pg_temp.review(a);
        RETURN;
    END IF;

    passed := p_stage <> 'REFUSEE_PRESELECTION';
    PERFORM pg_temp.preselect(a, CASE WHEN passed THEN 'VALIDEE' ELSE 'REFUSEE' END,
        pg_temp.pick(key, CASE WHEN passed THEN ARRAY[
            'Profil cohérent avec le poste, à approfondir en examen technique.',
            'Expérience solide sur les compétences demandées.',
            'Bon parcours et motivation claire.',
            'Profil complet, à confirmer en examen technique.']
        ELSE ARRAY[
            'Profil intéressant, mais trop éloigné des besoins actuels du poste.',
            'Expérience encore insuffisante sur les compétences obligatoires.',
            'Candidature solide, mais d''autres profils correspondent mieux au poste.'] END),
        p_applied + 1, 10);
    IF p_stage IN ('REFUSEE_PRESELECTION', 'EXAMEN_A_PLANIFIER') THEN
        RETURN;
    END IF;

    PERFORM pg_temp.book(a, 'TECHNIQUE', p_expert,
        CASE WHEN p_stage = 'EXAMEN_PLANIFIE' THEN p_next ELSE exam_day END, hour, p_applied + 2);
    IF p_stage = 'EXAMEN_PLANIFIE' THEN
        RETURN;
    END IF;

    passed := p_stage <> 'REFUSEE_TECHNIQUE';
    SELECT jsonb_object_agg(t.libelle, CASE WHEN passed THEN 5 + abs(hashtext(key || t.libelle)) % 5
                                            ELSE 2 + abs(hashtext(key || t.libelle)) % 4 END)
    INTO scores
    FROM exiger e JOIN trait t USING (id_trait)
    WHERE e.id_offre = o.id_offre
      AND (e.est_obligatoire OR EXISTS (SELECT 1 FROM posseder p
                                        WHERE p.id_candidat = pg_temp.account(p_email)
                                          AND p.id_trait = e.id_trait));
    PERFORM pg_temp.technical(a, CASE WHEN passed THEN 'VALIDEE' ELSE 'REFUSEE' END,
        pg_temp.pick(key, CASE WHEN passed THEN ARRAY[
            'Bonnes bases techniques et raisonnement clair. Quelques points à consolider.',
            'Très bon examen, à l''aise sur l''ensemble des sujets abordés.',
            'Solide sur les fondamentaux, explications claires et précises.']
        ELSE ARRAY[
            'Niveau encore insuffisant sur les compétences obligatoires du poste.',
            'Bonne volonté, mais trop de lacunes sur les sujets clés.'] END),
        scores);
    IF p_stage IN ('REFUSEE_TECHNIQUE', 'ENTRETIEN_A_PLANIFIER') THEN
        RETURN;
    END IF;

    PERFORM pg_temp.book(a, 'RH', NULL,
        CASE WHEN p_stage = 'ENTRETIEN_PLANIFIE' THEN p_next ELSE final_day END, hour, exam_day + 2);
    IF p_stage = 'ENTRETIEN_PLANIFIE' THEN
        RETURN;
    END IF;

    passed := p_stage = 'EMBAUCHEE';
    middle := COALESCE(round((o.salaire_min + o.salaire_max) / 2000) * 1000,
                       CASE WHEN o.type_contrat = 'INTERNSHIP' THEN 7200 ELSE 40000 END);
    PERFORM pg_temp.final(a, CASE WHEN passed THEN 'VALIDEE' ELSE 'REFUSEE' END,
        pg_temp.pick(key, CASE WHEN passed THEN ARRAY[
            'Échange très positif, attentes alignées avec le poste. Accord sur les conditions.',
            'Excellente adéquation avec l''équipe, démarrage rapide possible.']
        ELSE ARRAY[
            'Entretien correct, mais un autre profil a été retenu pour ce poste.',
            'Attentes de rémunération et de télétravail trop éloignées de l''offre.'] END),
        jsonb_build_object('salary', middle + CASE WHEN passed THEN 1000 ELSE 9000 END,
            'available', final_day + 30, 'contract', o.type_contrat, 'notice', '1 mois',
            'flexibility', 'Horaires flexibles', 'remote', COALESCE(o.modalite_teletravail, 'HYBRID'),
            'culture', pg_temp.pick(key, ARRAY[
                'Très bonne adéquation avec la culture de l''équipe.',
                'Profil autonome et communicant, bonne adéquation.'])),
        CASE WHEN passed THEN jsonb_build_object('salary', middle, 'start', final_day + 30,
            'contract', o.type_contrat, 'trial', CASE WHEN o.type_contrat = 'INTERNSHIP' THEN NULL ELSE '3 mois' END,
            'executive', CASE WHEN middle >= 45000 THEN 'CADRE' ELSE 'NON_CADRE' END,
            'benefits', pg_temp.pick(key, ARRAY[
                'Tickets restaurant, mutuelle, télétravail partiel',
                'Mutuelle, prime annuelle, budget formation'])) END);
END
$$;


-- The staff.

SELECT pg_temp.staff('rh@bridge.local', 'Claire', 'Moreau', 'RH', 'Ressources humaines', 0, NULL);
SELECT pg_temp.staff('expert@bridge.local', 'Yuki', 'Nakamura', 'EXPERT', 'Ingénierie logicielle', 0, NULL);
SELECT pg_temp.staff('rh2@bridge.local', 'Thomas', 'Lefèvre', 'RH', 'Ressources humaines', -120, 'France');
SELECT pg_temp.staff('expert2@bridge.local', 'Inès', 'Garnier', 'EXPERT', 'Données et cloud', -120, 'France');
SELECT pg_temp.staff('expert3@bridge.local', 'Mehdi', 'Rahmani', 'EXPERT', 'Front end et mobile', -120, 'France');


-- The candidates.

SELECT pg_temp.candidate('lea.martin@example.com', 'Léa', 'Martin', 'FEMME', '1996-04-12',
    '06 39 98 00 01', 'Lyon', 'BAC_5', -40,
    ARRAY['Java', 'Spring Boot', 'JPA', 'API REST', 'PostgreSQL', 'Docker', 'Tests unitaires', 'Git',
          'Français', 'Anglais', 'Confirmé (3 à 5 ans)', 'Esprit d''équipe', 'Rigueur']);
SELECT pg_temp.candidate('hugo.bernard@example.com', 'Hugo', 'Bernard', 'HOMME', '1999-09-03',
    '06 39 98 00 02', 'Paris', 'BAC_3', -100,
    ARRAY['React', 'Angular', 'TypeScript', 'JavaScript', 'HTML5', 'CSS3', 'Jest', 'Figma',
          'Accessibilité web (WCAG)', 'Français', 'Anglais', 'Junior (1 à 3 ans)', 'Créativité']);
SELECT pg_temp.candidate('amina.kaci@example.com', 'Amina', 'Kaci', 'FEMME', '1991-02-20',
    '06 39 98 00 03', 'Nantes', 'BAC_5', -25,
    ARRAY['Docker', 'Kubernetes', 'Terraform', 'CI/CD', 'AWS', 'Prometheus', 'Grafana',
          'Administration Linux', 'Python', 'Bash / Shell', 'Certification Kubernetes (CKA)',
          'Français', 'Anglais', 'Senior (5 à 8 ans)', 'Autonomie']);
SELECT pg_temp.candidate('lucas.petit@example.com', 'Lucas', 'Petit', 'HOMME', '1994-07-08',
    NULL, 'Toulouse', 'BAC_5', -35,
    ARRAY['Python', 'SQL', 'Apache Spark', 'Apache Kafka', 'Pandas', 'Data Engineering', 'AWS',
          'Français', 'Anglais', 'Confirmé (3 à 5 ans)', 'Esprit d''analyse']);
SELECT pg_temp.candidate('chloe.dubois@example.com', 'Chloé', 'Dubois', 'FEMME', '1998-11-30',
    '06 39 98 00 05', 'Lille', 'BAC_3', -28,
    ARRAY['JavaScript', 'TypeScript', 'Node.js', 'Vue.js', 'React', 'CSS3', 'PostgreSQL', 'Docker',
          'Français', 'Anglais', 'Junior (1 à 3 ans)', 'Curiosité technique']);
SELECT pg_temp.candidate('yanis.mercier@example.com', 'Yanis', 'Mercier', 'HOMME', '1993-05-17',
    '06 39 98 00 06', 'Lyon', 'BAC_5', -12,
    ARRAY['Java', 'Spring Boot', 'JPA', 'PostgreSQL', 'React', 'TypeScript', 'CSS3', 'Docker',
          'Kubernetes', 'Architecture logicielle', 'Français', 'Anglais', 'Senior (5 à 8 ans)',
          'Leadership']);
SELECT pg_temp.candidate('sarah.lambert@example.com', 'Sarah', 'Lambert', 'FEMME', '1995-03-22',
    '06 39 98 00 07', 'Marseille', 'BAC_3', -70,
    ARRAY['PHP', 'Symfony', 'MySQL', 'JavaScript', 'HTML5', 'CSS3', 'Git', 'Français',
          'Confirmé (3 à 5 ans)', 'Sens du service client']);
SELECT pg_temp.candidate('nathan.roux@example.com', 'Nathan', 'Roux', 'HOMME', '1997-12-02',
    NULL, 'Marseille', 'BAC_5', -68,
    ARRAY['PHP', 'Symfony', 'MySQL', 'Python', 'SQL', 'Apache Spark', 'Pandas', 'Français',
          'Anglais', 'Junior (1 à 3 ans)', 'Rigueur']);
SELECT pg_temp.candidate('manon.girard@example.com', 'Manon', 'Girard', 'FEMME', '2000-06-14',
    '06 39 98 00 09', 'Paris', 'BAC_3', -5,
    ARRAY['React', 'TypeScript', 'JavaScript', 'HTML5', 'CSS3', 'Tailwind CSS', 'Figma', 'Français',
          'Anglais', 'Débutant (moins de 1 an)', 'Créativité']);
SELECT pg_temp.candidate('karim.haddad@example.com', 'Karim', 'Haddad', 'HOMME', '1997-01-25',
    '06 39 98 00 10', 'Lille', 'BAC_3', -15,
    ARRAY['JavaScript', 'Node.js', 'Vue.js', 'Docker', 'Kubernetes', 'Terraform', 'CI/CD',
          'Français', 'Arabe', 'Anglais', 'Junior (1 à 3 ans)', 'Esprit d''initiative']);
SELECT pg_temp.candidate('paul.fontaine@example.com', 'Paul', 'Fontaine', 'HOMME', '1990-10-09',
    '06 39 98 00 11', 'Grenoble', 'BAC_5', -22,
    ARRAY['Java', 'Spring Boot', 'PostgreSQL', 'Kubernetes', 'Microservices',
          'Architecture logicielle', 'Français', 'Anglais', 'Expert (plus de 8 ans)', 'Leadership']);
SELECT pg_temp.candidate('emma.rousseau@example.com', 'Emma', 'Rousseau', 'FEMME', '1998-08-19',
    NULL, 'Paris', 'BAC_5', -11,
    ARRAY['React', 'TypeScript', 'Next.js', 'CSS3', 'Jest', 'Accessibilité web (WCAG)', 'Français',
          'Anglais', 'Espagnol', 'Junior (1 à 3 ans)', 'Communication']);
SELECT pg_temp.candidate('theo.garcia@example.com', 'Théo', 'Garcia', 'HOMME', '1995-04-02',
    '06 39 98 00 13', 'Toulouse', 'BAC_5', -14,
    ARRAY['Python', 'SQL', 'Apache Spark', 'Apache Kafka', 'AWS', 'Machine Learning', 'FastAPI',
          'PostgreSQL', 'Français', 'Anglais', 'Confirmé (3 à 5 ans)', 'Résolution de problèmes']);
SELECT pg_temp.candidate('jade.morel@example.com', 'Jade', 'Morel', 'FEMME', '1992-12-11',
    '06 39 98 00 14', 'Rennes', 'DOCTORAT', -9,
    ARRAY['Docker', 'Kubernetes', 'Terraform', 'CI/CD', 'Google Cloud Platform', 'Go', 'gRPC',
          'Français', 'Anglais', 'Senior (5 à 8 ans)', 'Publications scientifiques']);
SELECT pg_temp.candidate('camille.durand@example.com', 'Camille', 'Durand', 'FEMME', '1999-02-11',
    '06 39 98 00 15', 'Bordeaux', 'BAC_3', -6,
    ARRAY['Flutter', 'Dart', 'Firebase', 'UX mobile', 'Kotlin', 'Android', 'Français', 'Anglais',
          'Junior (1 à 3 ans)', 'Créativité']);
SELECT pg_temp.candidate('antoine.lefort@example.com', 'Antoine', 'Lefort', 'HOMME', '1994-09-27',
    '06 39 98 00 16', 'Paris', 'BAC_5', -8,
    ARRAY['Swift', 'iOS', 'Objective-C', 'Flutter', 'Dart', 'UX mobile', 'Git', 'Français', 'Anglais',
          'Confirmé (3 à 5 ans)', 'Rigueur']);
SELECT pg_temp.candidate('sofia.benali@example.com', 'Sofia', 'Benali', 'FEMME', '1996-06-05',
    '06 39 98 00 17', 'Paris', 'BAC_5', -4,
    ARRAY['Python', 'IA générative et LLM', 'Machine Learning', 'PyTorch', 'MLOps',
          'Traitement automatique du langage naturel', 'Docker', 'Français', 'Anglais', 'Arabe',
          'Confirmé (3 à 5 ans)', 'Curiosité technique']);
SELECT pg_temp.candidate('maxime.colin@example.com', 'Maxime', 'Colin', 'HOMME', '1990-01-19',
    NULL, 'Grenoble', 'DOCTORAT', -30,
    ARRAY['Python', 'SQL', 'Apache Spark', 'Machine Learning', 'IA générative et LLM', 'PyTorch',
          'Deep Learning', 'Statistiques', 'Français', 'Anglais', 'Senior (5 à 8 ans)',
          'Publications scientifiques']);
SELECT pg_temp.candidate('julie.faure@example.com', 'Julie', 'Faure', 'FEMME', '1987-03-14',
    '06 39 98 00 19', 'Lyon', 'BAC_5', -20,
    ARRAY['Java', 'Spring Boot', 'PostgreSQL', 'Architecture logicielle', 'Microservices',
          'Kubernetes', 'Mentorat', 'Leadership', 'Français', 'Anglais', 'Lead technique',
          'Expert (plus de 8 ans)']);
SELECT pg_temp.candidate('romain.giraud@example.com', 'Romain', 'Giraud', 'HOMME', '1993-11-08',
    '06 39 98 00 20', 'Nantes', 'BAC_3', -40,
    ARRAY['C#', '.NET Core', 'ASP.NET', 'SQL Server', 'Azure', 'Docker', 'Git', 'Français', 'Anglais',
          'Confirmé (3 à 5 ans)', 'Esprit d''équipe']);
SELECT pg_temp.candidate('clara.henry@example.com', 'Clara', 'Henry', 'FEMME', '1997-07-21',
    NULL, 'Toulouse', 'BAC_3', -10,
    ARRAY['Tests end to end', 'Cypress', 'Playwright', 'Selenium', 'JavaScript', 'Jest', 'CI/CD',
          'Français', 'Anglais', 'Junior (1 à 3 ans)', 'Rigueur']);
SELECT pg_temp.candidate('adrien.leroy@example.com', 'Adrien', 'Leroy', 'HOMME', '1992-04-30',
    '06 39 98 00 22', 'Toulouse', 'BAC_3', -25,
    ARRAY['Administration Linux', 'Bash / Shell', 'Réseaux informatiques', 'Ansible', 'Nginx',
          'Docker', 'Certification Red Hat', 'Français', 'Confirmé (3 à 5 ans)', 'Autonomie']);
SELECT pg_temp.candidate('eva.marchand@example.com', 'Eva', 'Marchand', 'FEMME', '1995-10-02',
    '06 39 98 00 23', 'Lille', 'BAC_5', -7,
    ARRAY['Python', 'FastAPI', 'Django', 'PostgreSQL', 'Redis', 'Docker', 'Tests unitaires',
          'Français', 'Anglais', 'Confirmé (3 à 5 ans)', 'Esprit d''analyse']);
SELECT pg_temp.candidate('samuel.diallo@example.com', 'Samuel', 'Diallo', 'HOMME', '1991-12-15',
    '06 39 98 00 24', 'Rennes', 'BAC_5', -18,
    ARRAY['Cybersécurité', 'Réponse à incident', 'Sécurité réseau', 'Suite ELK',
          'Investigation numérique', 'Certification CISSP', 'Python', 'Français', 'Anglais',
          'Senior (5 à 8 ans)', 'Gestion du stress']);
SELECT pg_temp.candidate('louise.perrin@example.com', 'Louise', 'Perrin', 'FEMME', '1994-05-23',
    NULL, 'Bordeaux', 'BAC_5', -9,
    ARRAY['Go', 'gRPC', 'Kubernetes', 'PostgreSQL', 'Prometheus', 'Docker', 'Systèmes distribués',
          'Français', 'Anglais', 'Confirmé (3 à 5 ans)', 'Résolution de problèmes']);
SELECT pg_temp.candidate('bastien.renard@example.com', 'Bastien', 'Renard', 'HOMME', '2004-08-09',
    '06 39 98 00 26', 'Lille', 'BAC_2', -95,
    ARRAY['JavaScript', 'HTML5', 'CSS3', 'React', 'Git', 'Français', 'Anglais', 'Alternance',
          'Curiosité technique']);
SELECT pg_temp.candidate('alice.schmitt@example.com', 'Alice', 'Schmitt', 'FEMME', '1996-02-28',
    '06 39 98 00 27', 'Paris', 'BAC_3', -85,
    ARRAY['Angular', 'TypeScript', 'HTML5', 'CSS3', 'Sass', 'Jest', 'Français', 'Anglais',
          'Confirmé (3 à 5 ans)', 'Communication']);
SELECT pg_temp.candidate('hamza.toure@example.com', 'Hamza', 'Touré', 'HOMME', '1989-06-17',
    '06 39 98 00 28', 'Lille', 'BAC_3', -22,
    ARRAY['Agile / Scrum', 'Certification Scrum Master', 'Kanban', 'Jira', 'Confluence',
          'Gestion de projet', 'Communication', 'Français', 'Anglais', 'Senior (5 à 8 ans)',
          'Leadership']);
SELECT pg_temp.candidate('oceane.vidal@example.com', 'Océane', 'Vidal', 'FEMME', '2003-01-12',
    NULL, 'Lyon', 'BAC_2', -95,
    ARRAY['HTML5', 'CSS3', 'JavaScript', 'React', 'Figma', 'Git', 'Français', 'Anglais', 'Stage',
          'Esprit d''équipe']);
SELECT pg_temp.candidate('victor.blanc@example.com', 'Victor', 'Blanc', 'HOMME', '1993-08-04',
    '06 39 98 00 30', 'Grenoble', 'BAC_5', -5,
    ARRAY['Go', 'gRPC', 'Rust', 'Kubernetes', 'Docker', 'Terraform', 'CI/CD', 'Français', 'Anglais',
          'Senior (5 à 8 ans)', 'Autonomie']);

SELECT pg_temp.education(email, title, school, field, from_year, to_year)
FROM (VALUES
    ('lea.martin@example.com', 'Master Informatique', 'Université Lyon 1', 'Génie logiciel', 2017, 2019),
    ('lea.martin@example.com', 'Licence Informatique', 'Université Lyon 1', 'Informatique', 2014, 2017),
    ('hugo.bernard@example.com', 'Licence professionnelle Métiers du web', 'IUT de Paris', 'Développement web', 2018, 2021),
    ('amina.kaci@example.com', 'Diplôme d''ingénieur', 'École d''ingénieurs de Nantes', 'Informatique et réseaux', 2010, 2015),
    ('lucas.petit@example.com', 'Master Science des données', 'Université de Toulouse', 'Data science', 2016, 2018),
    ('chloe.dubois@example.com', 'Licence Informatique', 'Université de Lille', 'Informatique', 2017, 2020),
    ('yanis.mercier@example.com', 'Diplôme d''ingénieur', 'INSA Lyon', 'Informatique', 2011, 2016),
    ('sarah.lambert@example.com', 'Licence professionnelle Développement web', 'Aix-Marseille Université', 'Développement web', 2013, 2016),
    ('nathan.roux@example.com', 'Master MIAGE', 'Aix-Marseille Université', 'Systèmes d''information', 2019, 2021),
    ('manon.girard@example.com', 'Licence Informatique', 'Université Paris Cité', 'Informatique', 2020, 2023),
    ('karim.haddad@example.com', 'Bachelor Développement logiciel', 'École du numérique de Lille', 'Développement logiciel', 2016, 2019),
    ('paul.fontaine@example.com', 'Diplôme d''ingénieur', 'Grenoble INP', 'Informatique', 2008, 2013),
    ('emma.rousseau@example.com', 'Master Interaction homme machine', 'Université Paris-Saclay', 'Informatique', 2020, 2022),
    ('theo.garcia@example.com', 'Master Statistique et informatique décisionnelle', 'Université de Toulouse', 'Statistiques', 2016, 2018),
    ('jade.morel@example.com', 'Doctorat en informatique', 'Université de Rennes', 'Systèmes distribués', 2015, 2018),
    ('jade.morel@example.com', 'Master Informatique', 'Université de Rennes', 'Systèmes distribués', 2013, 2015),
    ('camille.durand@example.com', 'Licence professionnelle Applications mobiles', 'IUT de Bordeaux', 'Développement mobile', 2018, 2021),
    ('antoine.lefort@example.com', 'Master Informatique', 'Sorbonne Université', 'Systèmes et applications mobiles', 2016, 2018),
    ('sofia.benali@example.com', 'Master Intelligence artificielle', 'Université Paris-Saclay', 'Apprentissage automatique', 2018, 2020),
    ('maxime.colin@example.com', 'Doctorat en apprentissage automatique', 'Université Grenoble Alpes', 'Intelligence artificielle', 2014, 2017),
    ('julie.faure@example.com', 'Diplôme d''ingénieur', 'École Centrale de Lyon', 'Informatique', 2005, 2010),
    ('romain.giraud@example.com', 'Licence professionnelle Développement .NET', 'IUT de Nantes', 'Développement logiciel', 2012, 2015),
    ('clara.henry@example.com', 'Licence professionnelle Qualité logicielle', 'IUT de Toulouse', 'Tests et qualité', 2017, 2020),
    ('adrien.leroy@example.com', 'Licence professionnelle Administration systèmes', 'IUT de Toulouse', 'Systèmes et réseaux', 2011, 2014),
    ('eva.marchand@example.com', 'Master Informatique', 'Université de Lille', 'Génie logiciel', 2016, 2018),
    ('samuel.diallo@example.com', 'Master Cybersécurité', 'Université de Rennes', 'Sécurité des systèmes', 2013, 2015),
    ('louise.perrin@example.com', 'Diplôme d''ingénieur', 'ENSEIRB-MATMECA', 'Informatique', 2013, 2017),
    ('bastien.renard@example.com', 'BTS Services informatiques aux organisations', 'Lycée Gaston Berger, Lille', 'Développement', 2022, 2024),
    ('alice.schmitt@example.com', 'Licence Informatique', 'Université de Strasbourg', 'Informatique', 2014, 2017),
    ('hamza.toure@example.com', 'Licence Management des systèmes d''information', 'Université de Lille', 'Gestion de projet', 2008, 2011),
    ('oceane.vidal@example.com', 'BTS Services informatiques aux organisations', 'Lycée La Martinière, Lyon', 'Développement', 2021, 2023),
    ('victor.blanc@example.com', 'Diplôme d''ingénieur', 'Grenoble INP', 'Systèmes distribués', 2011, 2016))
    AS rows (email, title, school, field, from_year, to_year);


-- The offers. rh@bridge.local publishes ten, rh2@bridge.local fourteen.

SELECT pg_temp.offer('rh@bridge.local', 'Développeur back end Java (H/F)', NULL, 'BAC_5', 'PERMANENT',
    'Lyon', 'HYBRID', 42000, 52000, -20, 'PUBLIEE', TRUE,
    ARRAY['Java', 'Spring Boot', 'PostgreSQL', 'Français'],
    ARRAY['Docker', 'Kubernetes', 'Tests unitaires', 'Anglais'],
'Vous rejoignez l''équipe plateforme, qui conçoit les services au cœur de notre produit : facturation, notifications et API publiques.

Au quotidien, vous concevez et faites évoluer des services Spring Boot, écrivez des requêtes PostgreSQL qui tiennent la charge, et participez aux revues de code. Les choix d''architecture se font en équipe, documentés et argumentés.

Deux jours de télétravail par semaine, une équipe de six développeurs, et un vrai temps consacré à la dette technique.');

SELECT pg_temp.offer('rh@bridge.local', 'Développeur front end React (H/F)', NULL, 'BAC_3', 'PERMANENT',
    'Paris', 'HYBRID', 40000, 50000, -14, 'PUBLIEE', TRUE,
    ARRAY['React', 'TypeScript', 'CSS3'],
    ARRAY['Accessibilité web (WCAG)', 'Jest', 'Figma', 'Anglais'],
'Nous cherchons quelqu''un qui aime autant le détail d''une interface que la propreté du code qui la porte.

Vous construisez les écrans de notre application web avec React et TypeScript, en lien direct avec notre designer. Vous veillez à l''accessibilité, à la performance et à la cohérence du système de composants.

Une base de code récente, des tests qui comptent, et une équipe qui relit chaque changement.');

SELECT pg_temp.offer('rh@bridge.local', 'Ingénieur DevOps (H/F)', 'Atelier Nord', 'BAC_5', 'PERMANENT',
    'Nantes', 'FULL_REMOTE', 48000, 60000, -9, 'PUBLIEE', FALSE,
    ARRAY['Docker', 'Kubernetes', 'Terraform', 'CI/CD'],
    ARRAY['AWS', 'Prometheus', 'Certification Kubernetes (CKA)', 'Anglais'],
'Atelier Nord fait tourner une vingtaine de services pour des clients industriels, et veut que chaque mise en production soit un non événement.

Vous faites évoluer l''infrastructure décrite en Terraform, les clusters Kubernetes et les chaînes d''intégration continue. Vous outillez l''observabilité et accompagnez les équipes de développement dans leurs déploiements.

Poste entièrement à distance, avec un séminaire d''équipe par trimestre à Nantes.');

SELECT pg_temp.offer('rh@bridge.local', 'Stage développement mobile Flutter', NULL, 'BAC_3', 'INTERNSHIP',
    'Bordeaux', 'ON_SITE', NULL, NULL, -3, 'BROUILLON', TRUE,
    ARRAY['Flutter', 'Dart'],
    ARRAY['UX mobile', 'Firebase'],
'Stage de six mois au sein de l''équipe mobile, pour participer à la refonte de notre application Flutter.

Vous développez de nouveaux écrans, écrivez les tests associés et participez aux rituels de l''équipe.');

SELECT pg_temp.offer('rh@bridge.local', 'Développeur mobile Flutter (H/F)', NULL, 'BAC_3', 'PERMANENT',
    'Bordeaux', 'HYBRID', 40000, 48000, -2, 'PUBLIEE', TRUE,
    ARRAY['Flutter', 'Dart'],
    ARRAY['Firebase', 'UX mobile', 'Anglais'],
'Notre application mobile est utilisée chaque jour par quarante mille personnes, sur iOS comme sur Android, à partir d''une seule base Flutter.

Vous concevez de nouveaux parcours avec notre designer, soignez les performances et l''accessibilité, et participez aux choix d''architecture de l''application.

Une équipe mobile de quatre personnes, des mises en production chaque semaine, et des retours utilisateurs lus par toute l''équipe.');

SELECT pg_temp.offer('rh@bridge.local', 'Ingénieur data et IA générative (H/F)', 'Helio Systèmes', 'BAC_5',
    'PERMANENT', 'Paris', 'HYBRID', 55000, 68000, -1, 'PUBLIEE', TRUE,
    ARRAY['Python', 'IA générative et LLM', 'Machine Learning'],
    ARRAY['PyTorch', 'MLOps', 'Traitement automatique du langage naturel', 'Anglais'],
'Helio Systèmes intègre des modèles de langage à ses outils de support client, et cherche quelqu''un pour les faire passer du prototype à la production.

Vous concevez les chaînes de traitement, évaluez les modèles sur des données réelles, et mettez en place le suivi de leur qualité dans le temps. Vous travaillez avec les équipes produit pour décider de ce qui mérite d''être automatisé.

Un sujet neuf, un budget de calcul réel, et une équipe qui publie ce qu''elle apprend.');

SELECT pg_temp.offer('rh@bridge.local', 'Tech lead Java (H/F)', NULL, 'BAC_5', 'PERMANENT',
    'Lyon', 'HYBRID', 60000, 72000, -4, 'PUBLIEE', TRUE,
    ARRAY['Java', 'Spring Boot', 'Architecture logicielle', 'Leadership'],
    ARRAY['Kubernetes', 'Microservices', 'Mentorat'],
'Vous prenez la responsabilité technique d''une équipe de six développeurs sur notre plateforme de paiement.

Vous arbitrez les choix d''architecture, accompagnez la montée en compétence de l''équipe et restez dans le code une bonne partie de la semaine. Vous travaillez main dans la main avec le product manager pour découper et prioriser.

Un poste qui compte, dans une équipe qui sait déjà livrer.');

SELECT pg_temp.offer('rh@bridge.local', 'Développeur .NET (H/F)', 'Atelier Nord', 'BAC_3', 'PERMANENT',
    'Nantes', 'ON_SITE', 40000, 50000, -25, 'PUBLIEE', TRUE,
    ARRAY['C#', '.NET Core', 'SQL Server'],
    ARRAY['Azure', 'Docker'],
'Atelier Nord développe les logiciels de pilotage de lignes de production pour ses clients industriels.

Vous faites évoluer des applications .NET critiques, au contact des équipes terrain, et participez à leur migration progressive vers Azure.');

SELECT pg_temp.offer('rh@bridge.local', 'Product designer (H/F)', NULL, 'BAC_3', 'FIXED_TERM',
    'Paris', 'HYBRID', 42000, 50000, -1, 'BROUILLON', TRUE,
    ARRAY['Figma', 'Accessibilité web (WCAG)', 'Communication'],
    ARRAY['Design responsive', 'UX mobile'],
'CDD de douze mois pour accompagner la refonte de notre espace client.

Vous menez la recherche utilisateur, concevez les parcours et faites vivre le système de design avec les développeurs.');

SELECT pg_temp.offer('rh@bridge.local', 'Stage développeur web (H/F)', NULL, 'BAC_2', 'INTERNSHIP',
    'Lyon', 'ON_SITE', NULL, NULL, -90, 'CLOTUREE', TRUE,
    ARRAY['HTML5', 'CSS3', 'JavaScript'],
    ARRAY['React', 'Git'],
'Stage de quatre mois dans l''équipe web, pour construire avec nous les pages publiques de notre produit.

Vous êtes accompagné par une développeuse confirmée et livrez du code relu dès la deuxième semaine.');

SELECT pg_temp.offer('rh2@bridge.local', 'Data engineer (H/F)', NULL, 'BAC_5', 'PERMANENT',
    'Toulouse', 'HYBRID', 45000, 55000, -18, 'PUBLIEE', TRUE,
    ARRAY['Python', 'SQL', 'Apache Spark'],
    ARRAY['Apache Kafka', 'AWS', 'Anglais'],
'Notre équipe données alimente les tableaux de bord de toute l''entreprise et les modèles de prévision de l''équipe produit.

Vous concevez et maintenez des pipelines Spark fiables, modélisez les données pour l''analyse et garantissez leur qualité de bout en bout. Vous travaillez avec les analystes comme avec les développeurs.

Une stack moderne, des volumes réels, et la liberté de proposer.');

SELECT pg_temp.offer('rh2@bridge.local', 'Développeur full stack Node.js et Vue (H/F)', 'Helio Systèmes',
    'BAC_3', 'FIXED_TERM', 'Lille', 'HYBRID', 38000, 44000, -11, 'PUBLIEE', TRUE,
    ARRAY['JavaScript', 'Node.js', 'Vue.js'],
    ARRAY['PostgreSQL', 'Docker', 'Tests end to end'],
'Helio Systèmes renforce son équipe pour un CDD de dix-huit mois, le temps de livrer la nouvelle version de son portail client.

Vous intervenez sur toute la chaîne : API Node.js, interface Vue.js, base PostgreSQL. Vous êtes associé aux échanges avec les clients pilotes.');

SELECT pg_temp.offer('rh2@bridge.local', 'Consultant sécurité applicative (H/F)', NULL, 'BAC_5', 'FREELANCE',
    'Paris', 'FULL_REMOTE', NULL, NULL, -13, 'PUBLIEE', TRUE,
    ARRAY['OWASP', 'Sécurité applicative', 'Tests d''intrusion'],
    ARRAY['Certification CEH', 'Anglais'],
'Mission de six mois pour auditer nos applications web et accompagner les équipes dans la correction des vulnérabilités.

Vous menez des tests d''intrusion, rédigez des rapports actionnables et formez les développeurs aux bonnes pratiques.');

SELECT pg_temp.offer('rh2@bridge.local', 'Développeur PHP Symfony (H/F)', NULL, 'BAC_3', 'PERMANENT',
    'Marseille', 'ON_SITE', 36000, 42000, -60, 'CLOTUREE', TRUE,
    ARRAY['PHP', 'Symfony', 'MySQL'],
    ARRAY['Docker', 'Tests unitaires'],
'Vous maintenez et faites évoluer notre back office Symfony, utilisé chaque jour par nos équipes commerciales.

Vous travaillez au plus près des utilisateurs, sur des évolutions courtes et visibles.');

SELECT pg_temp.offer('rh2@bridge.local', 'Administrateur systèmes Linux (H/F)', NULL, 'BAC_3', 'PERMANENT',
    'Toulouse', 'ON_SITE', 38000, 46000, -16, 'PUBLIEE', TRUE,
    ARRAY['Administration Linux', 'Bash / Shell', 'Réseaux informatiques'],
    ARRAY['Ansible', 'Nginx', 'Certification Red Hat'],
'Vous prenez en charge un parc de deux cents serveurs Linux, de leur installation à leur supervision.

Vous automatisez ce qui se répète, documentez ce qui compte, et participez à une astreinte partagée entre cinq personnes.');

SELECT pg_temp.offer('rh2@bridge.local', 'Développeur Python FastAPI (H/F)', 'Helio Systèmes', 'BAC_5',
    'PERMANENT', 'Lille', 'FULL_REMOTE', 45000, 55000, -3, 'PUBLIEE', TRUE,
    ARRAY['Python', 'FastAPI', 'PostgreSQL'],
    ARRAY['Docker', 'Redis', 'Tests unitaires'],
'Helio Systèmes construit les API qui relient ses outils internes à ceux de ses clients, et cherche un développeur Python pour les faire grandir.

Vous concevez des services FastAPI typés et testés, modélisez les données dans PostgreSQL, et veillez à ce que chaque endpoint reste rapide sous la charge.

Poste entièrement à distance, avec une semaine par trimestre réunie à Lille.');

SELECT pg_temp.offer('rh2@bridge.local', 'Ingénieur QA automatisation (H/F)', NULL, 'BAC_3', 'FIXED_TERM',
    'Toulouse', 'HYBRID', 38000, 45000, -7, 'PUBLIEE', TRUE,
    ARRAY['Tests end to end', 'Cypress', 'JavaScript'],
    ARRAY['Playwright', 'CI/CD', 'Selenium'],
'CDD de dix-huit mois pour bâtir notre stratégie de tests automatisés de bout en bout.

Vous écrivez les scénarios critiques, les branchez sur l''intégration continue et aidez les développeurs à les faire vivre.');

SELECT pg_temp.offer('rh2@bridge.local', 'Développeur iOS Swift (H/F)', 'Atelier Nord', 'BAC_5', 'PERMANENT',
    'Paris', 'HYBRID', 48000, 58000, -5, 'PUBLIEE', TRUE,
    ARRAY['Swift', 'iOS'],
    ARRAY['UX mobile', 'Anglais'],
'Atelier Nord lance une application iOS destinée aux techniciens de maintenance, pensée pour être utilisée avec des gants, en atelier, sans réseau.

Vous construisez l''application en Swift de bout en bout, de la synchronisation hors ligne aux détails d''interaction, avec un designer dédié.

Un produit neuf, des utilisateurs exigeants, et un vrai terrain pour l''ergonomie.');

SELECT pg_temp.offer('rh2@bridge.local', 'Analyste cybersécurité SOC (H/F)', NULL, 'BAC_5', 'PERMANENT',
    'Rennes', 'ON_SITE', 42000, 52000, -12, 'PUBLIEE', TRUE,
    ARRAY['Cybersécurité', 'Réponse à incident', 'Sécurité réseau'],
    ARRAY['Suite ELK', 'Certification CISSP', 'Anglais'],
'Vous rejoignez notre centre opérationnel de sécurité, qui surveille l''ensemble de nos systèmes de production.

Vous analysez les alertes, conduisez les investigations et améliorez nos règles de détection.');

SELECT pg_temp.offer('rh2@bridge.local', 'Développeur Go backend (H/F)', NULL, 'BAC_5', 'PERMANENT',
    'Bordeaux', 'FULL_REMOTE', 52000, 64000, -6, 'PUBLIEE', TRUE,
    ARRAY['Go', 'gRPC', 'Kubernetes'],
    ARRAY['PostgreSQL', 'Prometheus'],
'Notre moteur de routage traite plusieurs milliers de requêtes par seconde, et il est écrit en Go.

Vous faites évoluer ses services gRPC, mesurez avant d''optimiser, et participez à leur exploitation sur Kubernetes. Les décisions techniques s''écrivent et se discutent avant d''être prises.

Poste entièrement à distance, équipe répartie entre Bordeaux, Lyon et Lille.');

SELECT pg_temp.offer('rh2@bridge.local', 'Alternance développeur full stack (H/F)', NULL, 'BAC_2', 'FIXED_TERM',
    'Lille', 'HYBRID', 14000, 18000, -10, 'PUBLIEE', TRUE,
    ARRAY['JavaScript', 'HTML5', 'CSS3'],
    ARRAY['React', 'Node.js'],
'Alternance de deux ans, en rythme trois semaines en entreprise et une semaine en formation.

Vous développez de vraies fonctionnalités, accompagné par un tuteur, sur une application React et Node.js.');

SELECT pg_temp.offer('rh2@bridge.local', 'Data analyst (H/F)', NULL, 'BAC_3', 'PERMANENT',
    'Toulouse', 'HYBRID', 36000, 44000, -2, 'BROUILLON', TRUE,
    ARRAY['SQL', 'Business Intelligence', 'Visualisation de données'],
    ARRAY['Python', 'Statistiques'],
'Vous construisez les tableaux de bord des équipes commerciales et répondez à leurs questions par les données.');

SELECT pg_temp.offer('rh2@bridge.local', 'Développeur Angular (H/F)', NULL, 'BAC_3', 'PERMANENT',
    'Paris', 'HYBRID', 40000, 48000, -80, 'CLOTUREE', TRUE,
    ARRAY['Angular', 'TypeScript'],
    ARRAY['Sass', 'Jest'],
'Vous développez l''interface de notre outil de planification, utilisé par trois cents agences.

Vous travaillez en binôme avec un développeur back end et livrez toutes les deux semaines.');

SELECT pg_temp.offer('rh2@bridge.local', 'Scrum master (H/F)', 'Helio Systèmes', 'BAC_3', 'PERMANENT',
    'Lille', 'HYBRID', 45000, 55000, -20, 'PUBLIEE', TRUE,
    ARRAY['Agile / Scrum', 'Certification Scrum Master', 'Communication'],
    ARRAY['Jira', 'Kanban'],
'Vous accompagnez deux équipes de développement de Helio Systèmes dans leur fonctionnement agile.

Vous animez les rituels, levez les obstacles et aidez les équipes à mesurer ce qui compte vraiment.');


-- Saved offers.

INSERT INTO offre_enregistree (id_candidat, id_offre, date_enregistrement)
SELECT pg_temp.account(email), pg_temp.offer_id(title), pg_temp.at(shift, 20)
FROM (VALUES ('manon.girard@example.com', 'Développeur front end React (H/F)', -4),
             ('manon.girard@example.com', 'Ingénieur DevOps (H/F)', -3),
             ('manon.girard@example.com', 'Data engineer (H/F)', -2),
             ('manon.girard@example.com', 'Développeur mobile Flutter (H/F)', -1),
             ('manon.girard@example.com', 'Ingénieur data et IA générative (H/F)', -1),
             ('lea.martin@example.com', 'Ingénieur DevOps (H/F)', -6),
             ('lea.martin@example.com', 'Tech lead Java (H/F)', -3),
             ('sofia.benali@example.com', 'Développeur Python FastAPI (H/F)', -2),
             ('camille.durand@example.com', 'Développeur iOS Swift (H/F)', -4),
             ('eva.marchand@example.com', 'Ingénieur data et IA générative (H/F)', -1))
    AS saved (email, title, shift);


-- The applications. manon.girard@example.com has applied to nothing, so the
-- act of applying can be walked from her account.

DO $story$
DECLARE
    a INT;
BEGIN
    -- Développeur PHP Symfony, closed: one hire, one refusal at the exam.
    a := pg_temp.apply('sarah.lambert@example.com', 'Développeur PHP Symfony (H/F)', -58, 10);
    PERFORM pg_temp.preselect(a, 'VALIDEE', 'Quatre ans sur Symfony et une vraie culture du back office.', -56, 9);
    PERFORM pg_temp.book(a, 'TECHNIQUE', 'expert@bridge.local', -50, 10, -55);
    PERFORM pg_temp.technical(a, 'VALIDEE', 'Très à l''aise sur Symfony et Doctrine. Requêtes MySQL propres, bonne lecture du code existant.',
        '{"PHP": 8, "Symfony": 9, "MySQL": 7, "Tests unitaires": 6}');
    PERFORM pg_temp.book(a, 'RH', NULL, -45, 14, -49);
    PERFORM pg_temp.final(a, 'VALIDEE', 'Excellent échange, motivée par le contact avec les utilisateurs. Accord sur les conditions.',
        '{"salary": 42000, "available": -25, "contract": "PERMANENT", "notice": "1 mois", "flexibility": "Horaires flexibles", "remote": "ON_SITE", "culture": "Très bonne adéquation avec une équipe proche du terrain."}',
        '{"salary": 41000, "start": -20, "contract": "PERMANENT", "trial": "3 mois", "executive": "NON_CADRE", "benefits": "Tickets restaurant, mutuelle prise en charge à 70 %, dix jours de RTT"}');

    a := pg_temp.apply('nathan.roux@example.com', 'Développeur PHP Symfony (H/F)', -57, 15);
    PERFORM pg_temp.preselect(a, 'VALIDEE', 'Profil plutôt orienté données, mais bases PHP présentes.', -56, 10);
    PERFORM pg_temp.book(a, 'TECHNIQUE', 'expert@bridge.local', -50, 11, -55);
    PERFORM pg_temp.technical(a, 'REFUSEE', 'Connaissances de Symfony trop théoriques pour le poste. À recontacter sur une offre data.',
        '{"PHP": 5, "Symfony": 4, "MySQL": 6}');

    -- Développeur back end Java: a refusal after the final interview, one
    -- interview held and waiting to be recorded, one exam ahead.
    a := pg_temp.apply('paul.fontaine@example.com', 'Développeur back end Java (H/F)', -19, 11);
    PERFORM pg_temp.preselect(a, 'VALIDEE', 'Profil très senior, à confirmer sur les attentes.', -18, 9);
    PERFORM pg_temp.book(a, 'TECHNIQUE', 'expert@bridge.local', -12, 10, -17);
    PERFORM pg_temp.technical(a, 'VALIDEE', 'Niveau technique remarquable, en particulier sur l''architecture en microservices.',
        '{"Java": 9, "Spring Boot": 9, "PostgreSQL": 8, "Français": 10, "Kubernetes": 8}');
    PERFORM pg_temp.book(a, 'RH', NULL, -7, 15, -11);
    PERFORM pg_temp.final(a, 'REFUSEE', 'Très bon candidat, mais des attentes salariales hors de la fourchette du poste et un préavis de trois mois.',
        '{"salary": 68000, "available": 80, "contract": "PERMANENT", "notice": "3 mois", "flexibility": "Peu flexible", "remote": "FULL_REMOTE", "culture": "Recherche un poste plus orienté architecture."}',
        NULL);

    a := pg_temp.apply('lea.martin@example.com', 'Développeur back end Java (H/F)', -15, 9);
    PERFORM pg_temp.preselect(a, 'VALIDEE', 'Parcours Spring solide et projets bien présentés.', -14, 11);
    PERFORM pg_temp.book(a, 'TECHNIQUE', 'expert@bridge.local', -8, 10, -13);
    PERFORM pg_temp.technical(a, 'VALIDEE', 'Bonne maîtrise de Spring et de JPA, raisonnement clair sur les transactions. Docker à approfondir.',
        '{"Java": 8, "Spring Boot": 8, "PostgreSQL": 7, "Français": 10, "Docker": 5, "Tests unitaires": 7}');
    PERFORM pg_temp.book(a, 'RH', NULL, -1, 10, -6);

    a := pg_temp.apply('yanis.mercier@example.com', 'Développeur back end Java (H/F)', -6, 18);
    PERFORM pg_temp.preselect(a, 'VALIDEE', 'Expérience full stack utile pour l''équipe.', -5, 10);
    PERFORM pg_temp.book(a, 'TECHNIQUE', 'expert@bridge.local', 2, 11, -4);

    -- Développeur front end React: an exam past its hour with nothing recorded,
    -- an exam ahead, a refusal followed by a new application, and a new one.
    a := pg_temp.apply('hugo.bernard@example.com', 'Développeur front end React (H/F)', -12, 14);
    PERFORM pg_temp.preselect(a, 'VALIDEE', 'Portfolio soigné, attention réelle à l''accessibilité.', -10, 9);
    PERFORM pg_temp.book(a, 'TECHNIQUE', 'expert3@bridge.local', -1, 14, -8);

    a := pg_temp.apply('chloe.dubois@example.com', 'Développeur front end React (H/F)', -13, 20);
    PERFORM pg_temp.preselect(a, 'REFUSEE', 'Profil prometteur, mais une expérience React encore limitée pour ce poste.', -11, 10);
    PERFORM pg_temp.apply('chloe.dubois@example.com', 'Développeur front end React (H/F)', -2, 19);

    a := pg_temp.apply('emma.rousseau@example.com', 'Développeur front end React (H/F)', -9, 12);
    PERFORM pg_temp.preselect(a, 'VALIDEE', 'Master IHM, très bon dossier.', -7, 14);
    PERFORM pg_temp.book(a, 'TECHNIQUE', 'expert3@bridge.local', 1, 15, -5);

    PERFORM pg_temp.apply('yanis.mercier@example.com', 'Développeur front end React (H/F)', -1, 9);

    -- Ingénieur DevOps, an offer that lets an exam be recorded before its hour.
    a := pg_temp.apply('amina.kaci@example.com', 'Ingénieur DevOps (H/F)', -8, 8);
    PERFORM pg_temp.preselect(a, 'VALIDEE', 'Certification CKA et sept ans d''expérience.', -6, 9);
    PERFORM pg_temp.book(a, 'TECHNIQUE', 'expert2@bridge.local', 3, 14, -2);

    a := pg_temp.apply('jade.morel@example.com', 'Ingénieur DevOps (H/F)', -7, 21);
    PERFORM pg_temp.preselect(a, 'VALIDEE', 'Doctorat en systèmes distribués, profil rare.', -5, 15);
    PERFORM pg_temp.book(a, 'TECHNIQUE', 'expert@bridge.local', 5, 10, -3);

    -- Data engineer: a final interview ahead, an exam ahead, one in review.
    a := pg_temp.apply('lucas.petit@example.com', 'Data engineer (H/F)', -17, 10);
    PERFORM pg_temp.preselect(a, 'VALIDEE', 'Expérience Spark en production.', -15, 11);
    PERFORM pg_temp.book(a, 'TECHNIQUE', 'expert2@bridge.local', -6, 9, -13);
    PERFORM pg_temp.technical(a, 'VALIDEE', 'Pipelines bien pensés, bonne intuition des coûts. Kafka utilisé mais pas encore maîtrisé.',
        '{"Python": 9, "SQL": 8, "Apache Spark": 7, "Apache Kafka": 5, "AWS": 6}');
    PERFORM pg_temp.book(a, 'RH', NULL, 4, 15, -4);

    a := pg_temp.apply('theo.garcia@example.com', 'Data engineer (H/F)', -10, 17);
    PERFORM pg_temp.preselect(a, 'VALIDEE', 'Double compétence données et apprentissage automatique.', -8, 10);
    PERFORM pg_temp.book(a, 'TECHNIQUE', 'expert2@bridge.local', 2, 9, -6);

    a := pg_temp.apply('nathan.roux@example.com', 'Data engineer (H/F)', -3, 11);
    PERFORM pg_temp.review(a);

    -- Développeur full stack: a final interview to schedule, an exam to hand to
    -- an expert.
    a := pg_temp.apply('chloe.dubois@example.com', 'Développeur full stack Node.js et Vue (H/F)', -10, 9);
    PERFORM pg_temp.preselect(a, 'VALIDEE', 'Profil complet sur la stack demandée.', -9, 14);
    PERFORM pg_temp.book(a, 'TECHNIQUE', 'expert3@bridge.local', -4, 10, -8);
    PERFORM pg_temp.technical(a, 'VALIDEE', 'Très bonne aisance sur Vue et Node. Tests de bout en bout à renforcer.',
        '{"JavaScript": 8, "Node.js": 7, "Vue.js": 8, "PostgreSQL": 6, "Docker": 5}');

    a := pg_temp.apply('karim.haddad@example.com', 'Développeur full stack Node.js et Vue (H/F)', -5, 13);
    PERFORM pg_temp.preselect(a, 'VALIDEE', 'Bon profil, curieux et polyvalent.', -1, 10);
END
$story$;

-- The rest of the market, each application walked to the stage named.

SELECT pg_temp.journey('oceane.vidal@example.com', 'Stage développeur web (H/F)', -87, 'EMBAUCHEE', 'expert3@bridge.local');
SELECT pg_temp.journey('bastien.renard@example.com', 'Stage développeur web (H/F)', -88, 'REFUSEE_PRESELECTION');
SELECT pg_temp.journey('alice.schmitt@example.com', 'Développeur Angular (H/F)', -78, 'EMBAUCHEE', 'expert3@bridge.local');
SELECT pg_temp.journey('hugo.bernard@example.com', 'Développeur Angular (H/F)', -77, 'REFUSEE_FINALE', 'expert3@bridge.local');
SELECT pg_temp.journey('romain.giraud@example.com', 'Développeur .NET (H/F)', -22, 'ENTRETIEN_PLANIFIE', 'expert@bridge.local', 6);
SELECT pg_temp.journey('julie.faure@example.com', 'Développeur back end Java (H/F)', -18, 'REFUSEE_PRESELECTION');
SELECT pg_temp.journey('hamza.toure@example.com', 'Scrum master (H/F)', -18, 'ENTRETIEN_PLANIFIE', 'expert3@bridge.local', 2);
SELECT pg_temp.journey('maxime.colin@example.com', 'Data engineer (H/F)', -16, 'REFUSEE_TECHNIQUE', 'expert2@bridge.local');
SELECT pg_temp.journey('adrien.leroy@example.com', 'Administrateur systèmes Linux (H/F)', -15, 'ENTRETIEN_A_PLANIFIER', 'expert2@bridge.local');
SELECT pg_temp.journey('samuel.diallo@example.com', 'Analyste cybersécurité SOC (H/F)', -10, 'EXAMEN_PLANIFIE', 'expert2@bridge.local', 4);
SELECT pg_temp.journey('bastien.renard@example.com', 'Alternance développeur full stack (H/F)', -8, 'EXAMEN_PLANIFIE', 'expert3@bridge.local', 2);
SELECT pg_temp.journey('clara.henry@example.com', 'Ingénieur QA automatisation (H/F)', -6, 'EXAMEN_PLANIFIE', 'expert3@bridge.local', 1);
SELECT pg_temp.journey('jade.morel@example.com', 'Développeur Go backend (H/F)', -4, 'EN_REVUE');
SELECT pg_temp.journey('antoine.lefort@example.com', 'Développeur iOS Swift (H/F)', -4, 'EN_REVUE');
SELECT pg_temp.journey('julie.faure@example.com', 'Tech lead Java (H/F)', -3, 'EXAMEN_PLANIFIE', 'expert@bridge.local', 3);
SELECT pg_temp.journey('yanis.mercier@example.com', 'Tech lead Java (H/F)', -3, 'EXAMEN_A_PLANIFIER');
SELECT pg_temp.journey('paul.fontaine@example.com', 'Tech lead Java (H/F)', -2, 'EN_REVUE');
SELECT pg_temp.journey('louise.perrin@example.com', 'Développeur Go backend (H/F)', -3, 'NOUVELLE');
SELECT pg_temp.journey('victor.blanc@example.com', 'Développeur Go backend (H/F)', -2, 'NOUVELLE');
SELECT pg_temp.journey('eva.marchand@example.com', 'Développeur Python FastAPI (H/F)', -2, 'NOUVELLE');
SELECT pg_temp.journey('theo.garcia@example.com', 'Développeur Python FastAPI (H/F)', -1, 'NOUVELLE');
SELECT pg_temp.journey('antoine.lefort@example.com', 'Développeur mobile Flutter (H/F)', -2, 'NOUVELLE');
SELECT pg_temp.journey('camille.durand@example.com', 'Développeur mobile Flutter (H/F)', -1, 'NOUVELLE');
SELECT pg_temp.journey('sofia.benali@example.com', 'Ingénieur data et IA générative (H/F)', -1, 'NOUVELLE');
SELECT pg_temp.journey('maxime.colin@example.com', 'Ingénieur data et IA générative (H/F)', -1, 'NOUVELLE');


DROP FUNCTION pg_temp.journey, pg_temp.final, pg_temp.technical, pg_temp.hold, pg_temp.book,
    pg_temp.preselect, pg_temp.advance, pg_temp.refuse, pg_temp.review, pg_temp.apply,
    pg_temp.offer, pg_temp.education, pg_temp.candidate, pg_temp.staff, pg_temp.notice,
    pg_temp.name_of, pg_temp.offer_id, pg_temp.trait, pg_temp.account, pg_temp.pick,
    pg_temp.spoken, pg_temp.workday, pg_temp.at, pg_temp.password;
