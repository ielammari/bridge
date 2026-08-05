-- The candidate's academic path: the qualifications actually held, each from a
-- named institution over named years.
--
-- This is not the matching gate and does not replace it. candidat.diplome stays
-- the single scalar level (BAC through DOCTORAT) that an offer's requirement is
-- compared against, because a gate has to be one ordered value. A path is the
-- description behind that value, read by a recruiter rather than by the filter.

CREATE TABLE formation (
    id_formation  SERIAL       PRIMARY KEY,
    id_candidat   INT          NOT NULL,
    intitule      VARCHAR(150) NOT NULL,
    etablissement VARCHAR(150) NOT NULL,
    domaine       VARCHAR(150),
    annee_debut   SMALLINT     NOT NULL,
    -- Null while the qualification is still being read for.
    annee_fin     SMALLINT,
    CONSTRAINT fk_formation_candidat FOREIGN KEY (id_candidat)
        REFERENCES candidat (id_candidat) ON DELETE CASCADE,
    CONSTRAINT ck_formation_annee_debut CHECK (annee_debut BETWEEN 1950 AND 2100),
    CONSTRAINT ck_formation_annees CHECK (annee_fin IS NULL OR annee_fin >= annee_debut)
);

CREATE INDEX idx_formation_candidat ON formation (id_candidat);
