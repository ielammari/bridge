-- Baseline schema for the recruitment platform.

-- ---------------------------------------------------------------------------
-- Accounts: two level inheritance chain rooted at utilisateur.
-- ---------------------------------------------------------------------------

CREATE TABLE utilisateur (
    id_utilisateur    SERIAL       PRIMARY KEY,
    email             VARCHAR(150) NOT NULL UNIQUE,
    mot_de_passe      VARCHAR(255) NOT NULL,
    nom               VARCHAR(80)  NOT NULL,
    prenom            VARCHAR(80)  NOT NULL,
    telephone         VARCHAR(20),
    date_inscription  DATE         NOT NULL,
    role              VARCHAR(20)  NOT NULL,
    CONSTRAINT ck_utilisateur_role CHECK (role IN ('CANDIDAT', 'RH', 'EXPERT'))
);

CREATE TABLE candidat (
    id_candidat       INT          PRIMARY KEY,
    chemin_cv         VARCHAR(255),
    niveau_experience VARCHAR(40),
    diplome           VARCHAR(80),
    CONSTRAINT fk_candidat_utilisateur FOREIGN KEY (id_candidat)
        REFERENCES utilisateur (id_utilisateur) ON DELETE CASCADE
);

CREATE TABLE evaluateur (
    id_evaluateur INT PRIMARY KEY,
    CONSTRAINT fk_evaluateur_utilisateur FOREIGN KEY (id_evaluateur)
        REFERENCES utilisateur (id_utilisateur) ON DELETE CASCADE
);

CREATE TABLE responsable_rh (
    id_rh       INT PRIMARY KEY,
    departement VARCHAR(80),
    CONSTRAINT fk_rh_evaluateur FOREIGN KEY (id_rh)
        REFERENCES evaluateur (id_evaluateur) ON DELETE CASCADE
);

CREATE TABLE expert_technique (
    id_expert  INT PRIMARY KEY,
    specialite VARCHAR(80),
    CONSTRAINT fk_expert_evaluateur FOREIGN KEY (id_expert)
        REFERENCES evaluateur (id_evaluateur) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------------
-- Traits: the single ensemble backing all matching.
-- ---------------------------------------------------------------------------

CREATE TABLE categorie_trait (
    id_categorie SERIAL      PRIMARY KEY,
    libelle      VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE trait (
    id_trait     SERIAL       PRIMARY KEY,
    id_categorie INT          NOT NULL,
    libelle      VARCHAR(120) NOT NULL,
    CONSTRAINT fk_trait_categorie FOREIGN KEY (id_categorie)
        REFERENCES categorie_trait (id_categorie) ON DELETE RESTRICT,
    CONSTRAINT uq_trait_libelle_categorie UNIQUE (id_categorie, libelle)
);

-- ---------------------------------------------------------------------------
-- Offers.
-- Note: experience level and languages are traits (see exiger), never columns.
-- ---------------------------------------------------------------------------

CREATE TABLE offre (
    id_offre             SERIAL       PRIMARY KEY,
    titre                VARCHAR(150) NOT NULL,
    description          TEXT         NOT NULL,
    diplome_requis       VARCHAR(80)  NOT NULL,
    type_contrat         VARCHAR(30)  NOT NULL,
    localisation         VARCHAR(120),
    modalite_teletravail VARCHAR(30),
    salaire_min          NUMERIC(12, 2),
    salaire_max          NUMERIC(12, 2),
    date_publication     DATE         NOT NULL,
    statut               VARCHAR(20)  NOT NULL,
    id_rh                INT          NOT NULL,
    CONSTRAINT fk_offre_rh FOREIGN KEY (id_rh)
        REFERENCES responsable_rh (id_rh) ON DELETE RESTRICT,
    CONSTRAINT ck_offre_statut CHECK (statut IN ('BROUILLON', 'PUBLIEE', 'CLOTUREE')),
    CONSTRAINT ck_offre_salaire CHECK (salaire_min IS NULL OR salaire_max IS NULL OR salaire_min <= salaire_max)
);

-- Traits an offer looks for, each flagged required (true) or plus (false).
CREATE TABLE exiger (
    id_offre         INT     NOT NULL,
    id_trait         INT     NOT NULL,
    est_obligatoire  BOOLEAN NOT NULL,
    PRIMARY KEY (id_offre, id_trait),
    CONSTRAINT fk_exiger_offre FOREIGN KEY (id_offre)
        REFERENCES offre (id_offre) ON DELETE CASCADE,
    CONSTRAINT fk_exiger_trait FOREIGN KEY (id_trait)
        REFERENCES trait (id_trait) ON DELETE RESTRICT
);

-- Candidate trait profile.
CREATE TABLE posseder (
    id_candidat INT NOT NULL,
    id_trait    INT NOT NULL,
    niveau      VARCHAR(30),
    PRIMARY KEY (id_candidat, id_trait),
    CONSTRAINT fk_posseder_candidat FOREIGN KEY (id_candidat)
        REFERENCES candidat (id_candidat) ON DELETE CASCADE,
    CONSTRAINT fk_posseder_trait FOREIGN KEY (id_trait)
        REFERENCES trait (id_trait) ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------------
-- Applications. Never deleted: rejection and discard are status changes.
-- ---------------------------------------------------------------------------

CREATE TABLE candidature (
    id_candidature   SERIAL      PRIMARY KEY,
    date_candidature TIMESTAMP   NOT NULL,
    cv_joint         VARCHAR(255) NOT NULL,
    statut           VARCHAR(30) NOT NULL,
    id_candidat      INT         NOT NULL,
    id_offre         INT         NOT NULL,
    CONSTRAINT fk_candidature_candidat FOREIGN KEY (id_candidat)
        REFERENCES candidat (id_candidat) ON DELETE RESTRICT,
    CONSTRAINT fk_candidature_offre FOREIGN KEY (id_offre)
        REFERENCES offre (id_offre) ON DELETE RESTRICT,
    CONSTRAINT uq_candidature_candidat_offre UNIQUE (id_candidat, id_offre),
    CONSTRAINT ck_candidature_statut CHECK (statut IN
        ('NOUVELLE', 'EN_REVUE', 'EXAMEN_TECHNIQUE', 'ENTRETIEN_RH', 'REFUSEE', 'EMBAUCHEE'))
);

-- ---------------------------------------------------------------------------
-- Availability and scheduling.
-- A slot is occupied when a rendez_vous references it. There is no redundant
-- state column.
-- ---------------------------------------------------------------------------

CREATE TABLE creneau_disponibilite (
    id_creneau    SERIAL PRIMARY KEY,
    date_creneau  DATE   NOT NULL,
    heure_creneau TIME   NOT NULL,
    id_declarant  INT    NOT NULL,
    CONSTRAINT fk_creneau_declarant FOREIGN KEY (id_declarant)
        REFERENCES evaluateur (id_evaluateur) ON DELETE CASCADE,
    CONSTRAINT uq_creneau_declarant_date_heure UNIQUE (id_declarant, date_creneau, heure_creneau)
);

CREATE TABLE rendez_vous (
    id_rendez_vous SERIAL      PRIMARY KEY,
    type           VARCHAR(20) NOT NULL,
    statut         VARCHAR(20) NOT NULL,
    id_candidature INT         NOT NULL,
    id_creneau     INT         NOT NULL UNIQUE,
    CONSTRAINT fk_rdv_candidature FOREIGN KEY (id_candidature)
        REFERENCES candidature (id_candidature) ON DELETE CASCADE,
    CONSTRAINT fk_rdv_creneau FOREIGN KEY (id_creneau)
        REFERENCES creneau_disponibilite (id_creneau) ON DELETE RESTRICT,
    CONSTRAINT ck_rdv_type CHECK (type IN ('TECHNIQUE', 'RH')),
    CONSTRAINT ck_rdv_statut CHECK (statut IN ('PLANIFIE', 'REALISE', 'ANNULE'))
);

-- ---------------------------------------------------------------------------
-- Evaluations. One evaluation per (application, type).
-- ---------------------------------------------------------------------------

CREATE TABLE evaluation (
    id_evaluation   SERIAL      PRIMARY KEY,
    type            VARCHAR(20) NOT NULL,
    commentaire     TEXT,
    decision        VARCHAR(20) NOT NULL,
    date_evaluation TIMESTAMP   NOT NULL,
    id_candidature  INT         NOT NULL,
    id_evaluateur   INT         NOT NULL,
    id_rendez_vous  INT         UNIQUE,
    CONSTRAINT fk_evaluation_candidature FOREIGN KEY (id_candidature)
        REFERENCES candidature (id_candidature) ON DELETE CASCADE,
    CONSTRAINT fk_evaluation_evaluateur FOREIGN KEY (id_evaluateur)
        REFERENCES evaluateur (id_evaluateur) ON DELETE RESTRICT,
    CONSTRAINT fk_evaluation_rdv FOREIGN KEY (id_rendez_vous)
        REFERENCES rendez_vous (id_rendez_vous) ON DELETE SET NULL,
    CONSTRAINT uq_evaluation_candidature_type UNIQUE (id_candidature, type),
    CONSTRAINT ck_evaluation_type CHECK (type IN ('PRESELECTION', 'TECHNIQUE', 'ENTRETIEN_RH')),
    CONSTRAINT ck_evaluation_decision CHECK (decision IN ('VALIDEE', 'REFUSEE'))
);

-- Per trait scoring. Only technical evaluations carry these rows, enforced in
-- the service layer.
CREATE TABLE noter (
    id_evaluation INT      NOT NULL,
    id_trait      INT      NOT NULL,
    note          SMALLINT NOT NULL,
    PRIMARY KEY (id_evaluation, id_trait),
    CONSTRAINT fk_noter_evaluation FOREIGN KEY (id_evaluation)
        REFERENCES evaluation (id_evaluation) ON DELETE CASCADE,
    CONSTRAINT fk_noter_trait FOREIGN KEY (id_trait)
        REFERENCES trait (id_trait) ON DELETE RESTRICT,
    CONSTRAINT ck_noter_note CHECK (note BETWEEN 0 AND 30)
);

-- ---------------------------------------------------------------------------
-- HR interview data and hiring.
-- Interview data is kept even when the outcome is a refusal.
-- ---------------------------------------------------------------------------

CREATE TABLE entretien_rh (
    id_entretien_rh       SERIAL PRIMARY KEY,
    salaire_attendu       NUMERIC(12, 2),
    date_disponibilite    DATE,
    adequation_culture    TEXT,
    id_candidature        INT NOT NULL UNIQUE,
    type_contrat_envisage VARCHAR(30),
    duree_preavis         VARCHAR(30),
    flexibilite_horaire   VARCHAR(50),
    attentes_teletravail  VARCHAR(30),
    CONSTRAINT fk_entretien_candidature FOREIGN KEY (id_candidature)
        REFERENCES candidature (id_candidature) ON DELETE CASCADE
);

CREATE TABLE embauche (
    id_embauche         SERIAL         PRIMARY KEY,
    salaire_negocie     NUMERIC(12, 2) NOT NULL,
    date_prise_poste    DATE           NOT NULL,
    avantages           TEXT,
    id_candidature      INT            NOT NULL UNIQUE,
    type_contrat_final  VARCHAR(30)    NOT NULL,
    duree_periode_essai VARCHAR(30),
    statut_cadre        VARCHAR(20),
    CONSTRAINT fk_embauche_candidature FOREIGN KEY (id_candidature)
        REFERENCES candidature (id_candidature) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------------
-- Messaging. System notifications have no human sender (nullable expediteur).
-- ---------------------------------------------------------------------------

CREATE TABLE message (
    id_message        SERIAL    PRIMARY KEY,
    contenu           TEXT      NOT NULL,
    date_envoi        TIMESTAMP NOT NULL,
    lu                BOOLEAN   NOT NULL DEFAULT FALSE,
    id_expediteur     INT,
    id_destinataire   INT       NOT NULL,
    id_candidature    INT,
    type_notification VARCHAR(30),
    CONSTRAINT fk_message_expediteur FOREIGN KEY (id_expediteur)
        REFERENCES utilisateur (id_utilisateur) ON DELETE SET NULL,
    CONSTRAINT fk_message_destinataire FOREIGN KEY (id_destinataire)
        REFERENCES utilisateur (id_utilisateur) ON DELETE CASCADE,
    CONSTRAINT fk_message_candidature FOREIGN KEY (id_candidature)
        REFERENCES candidature (id_candidature) ON DELETE SET NULL
);

-- ---------------------------------------------------------------------------
-- Indexes supporting the main read paths.
-- ---------------------------------------------------------------------------

CREATE INDEX idx_offre_statut          ON offre (statut);
CREATE INDEX idx_offre_rh              ON offre (id_rh);
CREATE INDEX idx_candidature_offre     ON candidature (id_offre);
CREATE INDEX idx_candidature_candidat  ON candidature (id_candidat);
CREATE INDEX idx_candidature_statut    ON candidature (statut);
CREATE INDEX idx_creneau_declarant     ON creneau_disponibilite (id_declarant, date_creneau, heure_creneau);
CREATE INDEX idx_evaluation_candidature ON evaluation (id_candidature);
CREATE INDEX idx_message_destinataire  ON message (id_destinataire, lu);
CREATE INDEX idx_posseder_trait        ON posseder (id_trait);
CREATE INDEX idx_exiger_trait          ON exiger (id_trait);
