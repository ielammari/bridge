-- The current database schema.
--
-- Read this to learn the shape of the data. Do not edit it, and do not run
-- it: Flyway owns the schema through src/main/resources/db/migration, and
-- this file is regenerated from a database those migrations produced.


CREATE TABLE public.candidat (
    id_candidat integer NOT NULL,
    chemin_cv character varying(255),
    niveau_experience character varying(40),
    diplome character varying(80)
);

CREATE TABLE public.candidature (
    id_candidature integer NOT NULL,
    date_candidature timestamp without time zone NOT NULL,
    cv_joint character varying(255) NOT NULL,
    statut character varying(30) NOT NULL,
    id_candidat integer NOT NULL,
    id_offre integer NOT NULL,
    CONSTRAINT ck_candidature_statut CHECK (((statut)::text = ANY ((ARRAY['NOUVELLE'::character varying, 'EN_REVUE'::character varying, 'EXAMEN_TECHNIQUE'::character varying, 'ENTRETIEN_RH'::character varying, 'REFUSEE'::character varying, 'EMBAUCHEE'::character varying])::text[])))
);

CREATE SEQUENCE public.candidature_id_candidature_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.candidature_id_candidature_seq OWNED BY public.candidature.id_candidature;

CREATE TABLE public.categorie_trait (
    id_categorie integer NOT NULL,
    libelle character varying(80) NOT NULL,
    ordre smallint DEFAULT 999 NOT NULL
);

CREATE SEQUENCE public.categorie_trait_id_categorie_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.categorie_trait_id_categorie_seq OWNED BY public.categorie_trait.id_categorie;

CREATE TABLE public.cv (
    id_cv integer NOT NULL,
    id_candidat integer NOT NULL,
    intitule character varying(120) NOT NULL,
    chemin character varying(255) NOT NULL,
    date_depot timestamp without time zone NOT NULL
);

CREATE SEQUENCE public.cv_id_cv_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.cv_id_cv_seq OWNED BY public.cv.id_cv;

CREATE TABLE public.embauche (
    id_embauche integer NOT NULL,
    salaire_negocie numeric(12,2) NOT NULL,
    date_prise_poste date NOT NULL,
    avantages text,
    id_candidature integer NOT NULL,
    type_contrat_final character varying(30) NOT NULL,
    duree_periode_essai character varying(30),
    statut_cadre character varying(20)
);

CREATE SEQUENCE public.embauche_id_embauche_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.embauche_id_embauche_seq OWNED BY public.embauche.id_embauche;

CREATE TABLE public.entretien_rh (
    id_entretien_rh integer NOT NULL,
    salaire_attendu numeric(12,2),
    date_disponibilite date,
    adequation_culture text,
    id_candidature integer NOT NULL,
    type_contrat_envisage character varying(30),
    duree_preavis character varying(30),
    flexibilite_horaire character varying(50),
    attentes_teletravail character varying(30)
);

CREATE SEQUENCE public.entretien_rh_id_entretien_rh_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.entretien_rh_id_entretien_rh_seq OWNED BY public.entretien_rh.id_entretien_rh;

CREATE TABLE public.evaluateur (
    id_evaluateur integer NOT NULL
);

CREATE TABLE public.evaluation (
    id_evaluation integer NOT NULL,
    type character varying(20) NOT NULL,
    commentaire text,
    decision character varying(20) NOT NULL,
    date_evaluation timestamp without time zone NOT NULL,
    id_candidature integer NOT NULL,
    id_evaluateur integer NOT NULL,
    id_rendez_vous integer,
    CONSTRAINT ck_evaluation_decision CHECK (((decision)::text = ANY ((ARRAY['VALIDEE'::character varying, 'REFUSEE'::character varying])::text[]))),
    CONSTRAINT ck_evaluation_type CHECK (((type)::text = ANY ((ARRAY['PRESELECTION'::character varying, 'TECHNIQUE'::character varying, 'ENTRETIEN_RH'::character varying])::text[])))
);

CREATE SEQUENCE public.evaluation_id_evaluation_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.evaluation_id_evaluation_seq OWNED BY public.evaluation.id_evaluation;

CREATE TABLE public.exiger (
    id_offre integer NOT NULL,
    id_trait integer NOT NULL,
    est_obligatoire boolean NOT NULL
);

CREATE TABLE public.expert_technique (
    id_expert integer NOT NULL,
    specialite character varying(80)
);

CREATE TABLE public.formation (
    id_formation integer NOT NULL,
    id_candidat integer NOT NULL,
    intitule character varying(150) NOT NULL,
    etablissement character varying(150) NOT NULL,
    domaine character varying(150),
    annee_debut smallint NOT NULL,
    annee_fin smallint,
    CONSTRAINT ck_formation_annee_debut CHECK (((annee_debut >= 1950) AND (annee_debut <= 2100))),
    CONSTRAINT ck_formation_annees CHECK (((annee_fin IS NULL) OR (annee_fin >= annee_debut)))
);

CREATE SEQUENCE public.formation_id_formation_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.formation_id_formation_seq OWNED BY public.formation.id_formation;

CREATE TABLE public.message (
    id_message integer NOT NULL,
    contenu text NOT NULL,
    date_envoi timestamp without time zone NOT NULL,
    lu boolean DEFAULT false NOT NULL,
    id_expediteur integer,
    id_destinataire integer NOT NULL,
    id_candidature integer,
    type_notification character varying(30)
);

CREATE SEQUENCE public.message_id_message_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.message_id_message_seq OWNED BY public.message.id_message;

CREATE TABLE public.noter (
    id_evaluation integer NOT NULL,
    id_trait integer NOT NULL,
    note smallint NOT NULL,
    CONSTRAINT ck_noter_note CHECK (((note >= 0) AND (note <= 10)))
);

CREATE TABLE public.offre (
    id_offre integer NOT NULL,
    titre character varying(150) NOT NULL,
    description text NOT NULL,
    diplome_requis character varying(80) NOT NULL,
    type_contrat character varying(30) NOT NULL,
    localisation character varying(120),
    modalite_teletravail character varying(30),
    salaire_min numeric(12,2),
    salaire_max numeric(12,2),
    date_publication date NOT NULL,
    statut character varying(20) NOT NULL,
    id_rh integer NOT NULL,
    entreprise character varying(120) DEFAULT 'Bridge'::character varying NOT NULL,
    attendre_rendez_vous boolean DEFAULT true NOT NULL,
    CONSTRAINT ck_offre_salaire CHECK (((salaire_min IS NULL) OR (salaire_max IS NULL) OR (salaire_min <= salaire_max))),
    CONSTRAINT ck_offre_statut CHECK (((statut)::text = ANY ((ARRAY['BROUILLON'::character varying, 'PUBLIEE'::character varying, 'CLOTUREE'::character varying])::text[])))
);

CREATE TABLE public.offre_enregistree (
    id_candidat integer NOT NULL,
    id_offre integer NOT NULL,
    date_enregistrement timestamp without time zone NOT NULL
);

CREATE SEQUENCE public.offre_id_offre_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.offre_id_offre_seq OWNED BY public.offre.id_offre;

CREATE TABLE public.parametre_organisation (
    id smallint NOT NULL,
    premiere_heure smallint NOT NULL,
    derniere_heure smallint NOT NULL,
    CONSTRAINT ck_parametre_heures CHECK (((premiere_heure >= 0) AND (derniere_heure <= 23) AND (premiere_heure < derniere_heure))),
    CONSTRAINT ck_parametre_singleton CHECK ((id = 1))
);

CREATE TABLE public.posseder (
    id_candidat integer NOT NULL,
    id_trait integer NOT NULL,
    niveau character varying(30)
);

CREATE TABLE public.preference_notification (
    id_utilisateur integer NOT NULL,
    type_notification character varying(30) NOT NULL,
    CONSTRAINT ck_preference_type CHECK (((type_notification)::text = ANY ((ARRAY['APPLICATION_RECEIVED'::character varying, 'APPLICATION_SUBMITTED'::character varying, 'SCHEDULE_NEEDED'::character varying, 'EXAM_OVERDUE'::character varying])::text[])))
);

CREATE TABLE public.rendez_vous (
    id_rendez_vous integer NOT NULL,
    type character varying(20) NOT NULL,
    statut character varying(20) NOT NULL,
    id_candidature integer NOT NULL,
    date_rendez_vous date NOT NULL,
    heure_rendez_vous time without time zone NOT NULL,
    id_evaluateur integer NOT NULL,
    CONSTRAINT ck_rdv_statut CHECK (((statut)::text = ANY ((ARRAY['PLANIFIE'::character varying, 'REALISE'::character varying, 'ANNULE'::character varying])::text[]))),
    CONSTRAINT ck_rdv_type CHECK (((type)::text = ANY ((ARRAY['TECHNIQUE'::character varying, 'RH'::character varying])::text[])))
);

CREATE SEQUENCE public.rendez_vous_id_rendez_vous_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.rendez_vous_id_rendez_vous_seq OWNED BY public.rendez_vous.id_rendez_vous;

CREATE TABLE public.responsable_rh (
    id_rh integer NOT NULL,
    departement character varying(80)
);

CREATE TABLE public.trait (
    id_trait integer NOT NULL,
    id_categorie integer NOT NULL,
    libelle character varying(120) NOT NULL
);

CREATE SEQUENCE public.trait_id_trait_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.trait_id_trait_seq OWNED BY public.trait.id_trait;

CREATE TABLE public.utilisateur (
    id_utilisateur integer NOT NULL,
    email character varying(150) NOT NULL,
    mot_de_passe character varying(255),
    nom character varying(80) NOT NULL,
    prenom character varying(80) NOT NULL,
    telephone character varying(20),
    date_inscription date NOT NULL,
    role character varying(20) NOT NULL,
    date_naissance date,
    sexe character varying(20),
    ville character varying(100),
    pays character varying(100),
    mot_de_passe_a_changer boolean DEFAULT false NOT NULL,
    google_sub character varying(255),
    CONSTRAINT ck_utilisateur_date_naissance CHECK (((date_naissance IS NULL) OR (date_naissance < CURRENT_DATE))),
    CONSTRAINT ck_utilisateur_identite CHECK (((mot_de_passe IS NOT NULL) OR (google_sub IS NOT NULL))),
    CONSTRAINT ck_utilisateur_role CHECK (((role)::text = ANY ((ARRAY['CANDIDAT'::character varying, 'RH'::character varying, 'EXPERT'::character varying])::text[]))),
    CONSTRAINT ck_utilisateur_sexe CHECK (((sexe IS NULL) OR ((sexe)::text = ANY ((ARRAY['HOMME'::character varying, 'FEMME'::character varying, 'AUTRE'::character varying])::text[]))))
);

CREATE SEQUENCE public.utilisateur_id_utilisateur_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

ALTER SEQUENCE public.utilisateur_id_utilisateur_seq OWNED BY public.utilisateur.id_utilisateur;

ALTER TABLE ONLY public.candidature ALTER COLUMN id_candidature SET DEFAULT nextval('public.candidature_id_candidature_seq'::regclass);

ALTER TABLE ONLY public.categorie_trait ALTER COLUMN id_categorie SET DEFAULT nextval('public.categorie_trait_id_categorie_seq'::regclass);

ALTER TABLE ONLY public.cv ALTER COLUMN id_cv SET DEFAULT nextval('public.cv_id_cv_seq'::regclass);

ALTER TABLE ONLY public.embauche ALTER COLUMN id_embauche SET DEFAULT nextval('public.embauche_id_embauche_seq'::regclass);

ALTER TABLE ONLY public.entretien_rh ALTER COLUMN id_entretien_rh SET DEFAULT nextval('public.entretien_rh_id_entretien_rh_seq'::regclass);

ALTER TABLE ONLY public.evaluation ALTER COLUMN id_evaluation SET DEFAULT nextval('public.evaluation_id_evaluation_seq'::regclass);

ALTER TABLE ONLY public.formation ALTER COLUMN id_formation SET DEFAULT nextval('public.formation_id_formation_seq'::regclass);

ALTER TABLE ONLY public.message ALTER COLUMN id_message SET DEFAULT nextval('public.message_id_message_seq'::regclass);

ALTER TABLE ONLY public.offre ALTER COLUMN id_offre SET DEFAULT nextval('public.offre_id_offre_seq'::regclass);

ALTER TABLE ONLY public.rendez_vous ALTER COLUMN id_rendez_vous SET DEFAULT nextval('public.rendez_vous_id_rendez_vous_seq'::regclass);

ALTER TABLE ONLY public.trait ALTER COLUMN id_trait SET DEFAULT nextval('public.trait_id_trait_seq'::regclass);

ALTER TABLE ONLY public.utilisateur ALTER COLUMN id_utilisateur SET DEFAULT nextval('public.utilisateur_id_utilisateur_seq'::regclass);

ALTER TABLE ONLY public.candidat
    ADD CONSTRAINT candidat_pkey PRIMARY KEY (id_candidat);

ALTER TABLE ONLY public.candidature
    ADD CONSTRAINT candidature_pkey PRIMARY KEY (id_candidature);

ALTER TABLE ONLY public.categorie_trait
    ADD CONSTRAINT categorie_trait_libelle_key UNIQUE (libelle);

ALTER TABLE ONLY public.categorie_trait
    ADD CONSTRAINT categorie_trait_pkey PRIMARY KEY (id_categorie);

ALTER TABLE ONLY public.cv
    ADD CONSTRAINT cv_pkey PRIMARY KEY (id_cv);

ALTER TABLE ONLY public.embauche
    ADD CONSTRAINT embauche_id_candidature_key UNIQUE (id_candidature);

ALTER TABLE ONLY public.embauche
    ADD CONSTRAINT embauche_pkey PRIMARY KEY (id_embauche);

ALTER TABLE ONLY public.entretien_rh
    ADD CONSTRAINT entretien_rh_id_candidature_key UNIQUE (id_candidature);

ALTER TABLE ONLY public.entretien_rh
    ADD CONSTRAINT entretien_rh_pkey PRIMARY KEY (id_entretien_rh);

ALTER TABLE ONLY public.evaluateur
    ADD CONSTRAINT evaluateur_pkey PRIMARY KEY (id_evaluateur);

ALTER TABLE ONLY public.evaluation
    ADD CONSTRAINT evaluation_id_rendez_vous_key UNIQUE (id_rendez_vous);

ALTER TABLE ONLY public.evaluation
    ADD CONSTRAINT evaluation_pkey PRIMARY KEY (id_evaluation);

ALTER TABLE ONLY public.exiger
    ADD CONSTRAINT exiger_pkey PRIMARY KEY (id_offre, id_trait);

ALTER TABLE ONLY public.expert_technique
    ADD CONSTRAINT expert_technique_pkey PRIMARY KEY (id_expert);

ALTER TABLE ONLY public.formation
    ADD CONSTRAINT formation_pkey PRIMARY KEY (id_formation);

ALTER TABLE ONLY public.message
    ADD CONSTRAINT message_pkey PRIMARY KEY (id_message);

ALTER TABLE ONLY public.noter
    ADD CONSTRAINT noter_pkey PRIMARY KEY (id_evaluation, id_trait);

ALTER TABLE ONLY public.offre_enregistree
    ADD CONSTRAINT offre_enregistree_pkey PRIMARY KEY (id_candidat, id_offre);

ALTER TABLE ONLY public.offre
    ADD CONSTRAINT offre_pkey PRIMARY KEY (id_offre);

ALTER TABLE ONLY public.parametre_organisation
    ADD CONSTRAINT parametre_organisation_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.posseder
    ADD CONSTRAINT posseder_pkey PRIMARY KEY (id_candidat, id_trait);

ALTER TABLE ONLY public.preference_notification
    ADD CONSTRAINT preference_notification_pkey PRIMARY KEY (id_utilisateur, type_notification);

ALTER TABLE ONLY public.rendez_vous
    ADD CONSTRAINT rendez_vous_pkey PRIMARY KEY (id_rendez_vous);

ALTER TABLE ONLY public.responsable_rh
    ADD CONSTRAINT responsable_rh_pkey PRIMARY KEY (id_rh);

ALTER TABLE ONLY public.trait
    ADD CONSTRAINT trait_pkey PRIMARY KEY (id_trait);

ALTER TABLE ONLY public.evaluation
    ADD CONSTRAINT uq_evaluation_candidature_type UNIQUE (id_candidature, type);

ALTER TABLE ONLY public.rendez_vous
    ADD CONSTRAINT uq_rdv_evaluateur_creneau UNIQUE (id_evaluateur, date_rendez_vous, heure_rendez_vous);

ALTER TABLE ONLY public.trait
    ADD CONSTRAINT uq_trait_libelle_categorie UNIQUE (id_categorie, libelle);

ALTER TABLE ONLY public.utilisateur
    ADD CONSTRAINT uq_utilisateur_google_sub UNIQUE (google_sub);

ALTER TABLE ONLY public.utilisateur
    ADD CONSTRAINT utilisateur_email_key UNIQUE (email);

ALTER TABLE ONLY public.utilisateur
    ADD CONSTRAINT utilisateur_pkey PRIMARY KEY (id_utilisateur);

CREATE INDEX idx_candidature_candidat ON public.candidature USING btree (id_candidat);

CREATE INDEX idx_candidature_offre ON public.candidature USING btree (id_offre);

CREATE INDEX idx_candidature_statut ON public.candidature USING btree (statut);

CREATE INDEX idx_cv_candidat ON public.cv USING btree (id_candidat);

CREATE INDEX idx_enregistree_candidat ON public.offre_enregistree USING btree (id_candidat);

CREATE INDEX idx_evaluation_candidature ON public.evaluation USING btree (id_candidature);

CREATE INDEX idx_exiger_trait ON public.exiger USING btree (id_trait);

CREATE INDEX idx_formation_candidat ON public.formation USING btree (id_candidat);

CREATE INDEX idx_message_destinataire ON public.message USING btree (id_destinataire, lu);

CREATE INDEX idx_offre_rh ON public.offre USING btree (id_rh);

CREATE INDEX idx_offre_statut ON public.offre USING btree (statut);

CREATE INDEX idx_posseder_trait ON public.posseder USING btree (id_trait);

CREATE UNIQUE INDEX uq_candidature_candidat_offre_active ON public.candidature USING btree (id_candidat, id_offre) WHERE ((statut)::text <> 'REFUSEE'::text);

ALTER TABLE ONLY public.candidat
    ADD CONSTRAINT fk_candidat_utilisateur FOREIGN KEY (id_candidat) REFERENCES public.utilisateur(id_utilisateur) ON DELETE CASCADE;

ALTER TABLE ONLY public.candidature
    ADD CONSTRAINT fk_candidature_candidat FOREIGN KEY (id_candidat) REFERENCES public.candidat(id_candidat) ON DELETE RESTRICT;

ALTER TABLE ONLY public.candidature
    ADD CONSTRAINT fk_candidature_offre FOREIGN KEY (id_offre) REFERENCES public.offre(id_offre) ON DELETE RESTRICT;

ALTER TABLE ONLY public.cv
    ADD CONSTRAINT fk_cv_candidat FOREIGN KEY (id_candidat) REFERENCES public.candidat(id_candidat) ON DELETE CASCADE;

ALTER TABLE ONLY public.embauche
    ADD CONSTRAINT fk_embauche_candidature FOREIGN KEY (id_candidature) REFERENCES public.candidature(id_candidature) ON DELETE CASCADE;

ALTER TABLE ONLY public.offre_enregistree
    ADD CONSTRAINT fk_enregistree_candidat FOREIGN KEY (id_candidat) REFERENCES public.candidat(id_candidat) ON DELETE CASCADE;

ALTER TABLE ONLY public.offre_enregistree
    ADD CONSTRAINT fk_enregistree_offre FOREIGN KEY (id_offre) REFERENCES public.offre(id_offre) ON DELETE CASCADE;

ALTER TABLE ONLY public.entretien_rh
    ADD CONSTRAINT fk_entretien_candidature FOREIGN KEY (id_candidature) REFERENCES public.candidature(id_candidature) ON DELETE CASCADE;

ALTER TABLE ONLY public.evaluateur
    ADD CONSTRAINT fk_evaluateur_utilisateur FOREIGN KEY (id_evaluateur) REFERENCES public.utilisateur(id_utilisateur) ON DELETE CASCADE;

ALTER TABLE ONLY public.evaluation
    ADD CONSTRAINT fk_evaluation_candidature FOREIGN KEY (id_candidature) REFERENCES public.candidature(id_candidature) ON DELETE CASCADE;

ALTER TABLE ONLY public.evaluation
    ADD CONSTRAINT fk_evaluation_evaluateur FOREIGN KEY (id_evaluateur) REFERENCES public.evaluateur(id_evaluateur) ON DELETE RESTRICT;

ALTER TABLE ONLY public.evaluation
    ADD CONSTRAINT fk_evaluation_rdv FOREIGN KEY (id_rendez_vous) REFERENCES public.rendez_vous(id_rendez_vous) ON DELETE SET NULL;

ALTER TABLE ONLY public.exiger
    ADD CONSTRAINT fk_exiger_offre FOREIGN KEY (id_offre) REFERENCES public.offre(id_offre) ON DELETE CASCADE;

ALTER TABLE ONLY public.exiger
    ADD CONSTRAINT fk_exiger_trait FOREIGN KEY (id_trait) REFERENCES public.trait(id_trait) ON DELETE RESTRICT;

ALTER TABLE ONLY public.expert_technique
    ADD CONSTRAINT fk_expert_evaluateur FOREIGN KEY (id_expert) REFERENCES public.evaluateur(id_evaluateur) ON DELETE CASCADE;

ALTER TABLE ONLY public.formation
    ADD CONSTRAINT fk_formation_candidat FOREIGN KEY (id_candidat) REFERENCES public.candidat(id_candidat) ON DELETE CASCADE;

ALTER TABLE ONLY public.message
    ADD CONSTRAINT fk_message_candidature FOREIGN KEY (id_candidature) REFERENCES public.candidature(id_candidature) ON DELETE SET NULL;

ALTER TABLE ONLY public.message
    ADD CONSTRAINT fk_message_destinataire FOREIGN KEY (id_destinataire) REFERENCES public.utilisateur(id_utilisateur) ON DELETE CASCADE;

ALTER TABLE ONLY public.message
    ADD CONSTRAINT fk_message_expediteur FOREIGN KEY (id_expediteur) REFERENCES public.utilisateur(id_utilisateur) ON DELETE SET NULL;

ALTER TABLE ONLY public.noter
    ADD CONSTRAINT fk_noter_evaluation FOREIGN KEY (id_evaluation) REFERENCES public.evaluation(id_evaluation) ON DELETE CASCADE;

ALTER TABLE ONLY public.noter
    ADD CONSTRAINT fk_noter_trait FOREIGN KEY (id_trait) REFERENCES public.trait(id_trait) ON DELETE RESTRICT;

ALTER TABLE ONLY public.offre
    ADD CONSTRAINT fk_offre_rh FOREIGN KEY (id_rh) REFERENCES public.responsable_rh(id_rh) ON DELETE RESTRICT;

ALTER TABLE ONLY public.posseder
    ADD CONSTRAINT fk_posseder_candidat FOREIGN KEY (id_candidat) REFERENCES public.candidat(id_candidat) ON DELETE CASCADE;

ALTER TABLE ONLY public.posseder
    ADD CONSTRAINT fk_posseder_trait FOREIGN KEY (id_trait) REFERENCES public.trait(id_trait) ON DELETE RESTRICT;

ALTER TABLE ONLY public.preference_notification
    ADD CONSTRAINT fk_preference_utilisateur FOREIGN KEY (id_utilisateur) REFERENCES public.utilisateur(id_utilisateur) ON DELETE CASCADE;

ALTER TABLE ONLY public.rendez_vous
    ADD CONSTRAINT fk_rdv_candidature FOREIGN KEY (id_candidature) REFERENCES public.candidature(id_candidature) ON DELETE CASCADE;

ALTER TABLE ONLY public.rendez_vous
    ADD CONSTRAINT fk_rdv_evaluateur FOREIGN KEY (id_evaluateur) REFERENCES public.evaluateur(id_evaluateur);

ALTER TABLE ONLY public.responsable_rh
    ADD CONSTRAINT fk_rh_evaluateur FOREIGN KEY (id_rh) REFERENCES public.evaluateur(id_evaluateur) ON DELETE CASCADE;

ALTER TABLE ONLY public.trait
    ADD CONSTRAINT fk_trait_categorie FOREIGN KEY (id_categorie) REFERENCES public.categorie_trait(id_categorie) ON DELETE RESTRICT;

