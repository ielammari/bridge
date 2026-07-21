-- Development accounts for the two roles that public signup cannot create.
-- Both use the password Bridge123! and must be removed or rotated before any
-- deployment outside local development.

INSERT INTO utilisateur (email, mot_de_passe, nom, prenom, telephone, date_inscription, role)
VALUES ('rh@bridge.local',
        '{bcrypt}$2a$10$5gpc0XV73c8DM..7l88o5O6K79B7LkACA7QLlIZT.Iz.Ln4GrDuM6',
        'Moreau', 'Claire', NULL, CURRENT_DATE, 'RH');

INSERT INTO evaluateur (id_evaluateur)
SELECT id_utilisateur FROM utilisateur WHERE email = 'rh@bridge.local';

INSERT INTO responsable_rh (id_rh, departement)
SELECT id_utilisateur, 'Ressources humaines' FROM utilisateur WHERE email = 'rh@bridge.local';

INSERT INTO utilisateur (email, mot_de_passe, nom, prenom, telephone, date_inscription, role)
VALUES ('expert@bridge.local',
        '{bcrypt}$2a$10$WIFCgof0196u5Hw4PvNyKOF6GU8ViFFroOyb59B2VHiie4E5YBCzi',
        'Nakamura', 'Yuki', NULL, CURRENT_DATE, 'EXPERT');

INSERT INTO evaluateur (id_evaluateur)
SELECT id_utilisateur FROM utilisateur WHERE email = 'expert@bridge.local';

INSERT INTO expert_technique (id_expert, specialite)
SELECT id_utilisateur, 'Ingénierie logicielle' FROM utilisateur WHERE email = 'expert@bridge.local';
