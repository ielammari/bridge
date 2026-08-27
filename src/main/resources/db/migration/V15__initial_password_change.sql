-- An account created by a recruiter starts on a password that recruiter chose,
-- so its holder is made to replace it before they can use it. Accounts that
-- already exist chose their own, including the development ones.

ALTER TABLE utilisateur
    ADD COLUMN mot_de_passe_a_changer boolean NOT NULL DEFAULT false;
