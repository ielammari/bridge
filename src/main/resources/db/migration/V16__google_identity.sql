-- An account authenticates by a password, by a linked Google identity, or by
-- both, so the password becomes optional and the Google subject sits beside it.

ALTER TABLE utilisateur
    ALTER COLUMN mot_de_passe DROP NOT NULL;

ALTER TABLE utilisateur
    ADD COLUMN google_sub VARCHAR(255);

ALTER TABLE utilisateur
    ADD CONSTRAINT uq_utilisateur_google_sub UNIQUE (google_sub);

-- Neither means of signing in leaves an account that nobody can reach.
ALTER TABLE utilisateur
    ADD CONSTRAINT ck_utilisateur_identite
        CHECK (mot_de_passe IS NOT NULL OR google_sub IS NOT NULL);
