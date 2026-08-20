-- Offers a candidate keeps to come back to, and the right to apply again after
-- a refusal.
--
-- The unique pair becomes partial: a candidate still cannot hold two live
-- applications on one offer, but a refused one no longer blocks a fresh
-- attempt. A plain constraint cannot express that, so it gives way to a partial
-- unique index.

CREATE TABLE offre_enregistree (
    id_candidat        INT       NOT NULL,
    id_offre           INT       NOT NULL,
    date_enregistrement TIMESTAMP NOT NULL,
    PRIMARY KEY (id_candidat, id_offre),
    CONSTRAINT fk_enregistree_candidat FOREIGN KEY (id_candidat)
        REFERENCES candidat (id_candidat) ON DELETE CASCADE,
    CONSTRAINT fk_enregistree_offre FOREIGN KEY (id_offre)
        REFERENCES offre (id_offre) ON DELETE CASCADE
);

CREATE INDEX idx_enregistree_candidat ON offre_enregistree (id_candidat);

ALTER TABLE candidature
    DROP CONSTRAINT uq_candidature_candidat_offre;

CREATE UNIQUE INDEX uq_candidature_candidat_offre_active
    ON candidature (id_candidat, id_offre)
    WHERE statut <> 'REFUSEE';
