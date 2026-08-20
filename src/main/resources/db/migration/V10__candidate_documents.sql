-- A candidate keeps several CVs and chooses one each time they apply, so a
-- profile is no longer limited to a single current document.
--
-- candidat.chemin_cv stays as the default: the one already on file becomes the
-- first document, and the column keeps pointing at whichever is currently
-- proposed when applying.

CREATE TABLE cv (
    id_cv       SERIAL       PRIMARY KEY,
    id_candidat INT          NOT NULL,
    intitule    VARCHAR(120) NOT NULL,
    chemin      VARCHAR(255) NOT NULL,
    date_depot  TIMESTAMP    NOT NULL,
    CONSTRAINT fk_cv_candidat FOREIGN KEY (id_candidat)
        REFERENCES candidat (id_candidat) ON DELETE CASCADE
);

CREATE INDEX idx_cv_candidat ON cv (id_candidat);

INSERT INTO cv (id_candidat, intitule, chemin, date_depot)
SELECT id_candidat, 'CV', chemin_cv, CURRENT_TIMESTAMP
FROM candidat
WHERE chemin_cv IS NOT NULL;
