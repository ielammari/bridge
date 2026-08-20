-- The company an offer recruits for, named by the recruiter when they configure
-- it. Existing offers take the default, which is also what the form proposes.

ALTER TABLE offre
    ADD COLUMN entreprise VARCHAR(120) NOT NULL DEFAULT 'Bridge';
