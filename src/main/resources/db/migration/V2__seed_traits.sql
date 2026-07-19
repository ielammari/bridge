-- Seed of the traits catalogue.
-- Traits are the single ensemble covering technical skills, experience level,
-- languages, and bonus criteria. HR picks from this table when configuring an
-- offer; candidates pick from it when building their profile.

INSERT INTO categorie_trait (libelle) VALUES
    ('Compétences techniques'),
    ('Niveau d''expérience'),
    ('Langues'),
    ('Atouts');

-- ---------------------------------------------------------------------------
-- Compétences techniques
-- ---------------------------------------------------------------------------

INSERT INTO trait (id_categorie, libelle)
SELECT c.id_categorie, t.libelle
FROM categorie_trait c
CROSS JOIN (VALUES
    -- Langages de programmation
    ('Java'), ('Python'), ('JavaScript'), ('TypeScript'), ('C'), ('C++'),
    ('C#'), ('Go'), ('Rust'), ('Kotlin'), ('Swift'), ('Ruby'), ('PHP'),
    ('Scala'), ('Perl'), ('R'), ('MATLAB'), ('Dart'), ('Elixir'), ('Haskell'),
    ('Clojure'), ('F#'), ('Groovy'), ('Lua'), ('Julia'), ('Objective-C'),
    ('Visual Basic .NET'), ('Assembleur'), ('COBOL'), ('Fortran'), ('Ada'),
    ('Erlang'), ('OCaml'), ('Racket'), ('Scheme'), ('Prolog'), ('Solidity'),
    ('Zig'), ('Nim'), ('Crystal'), ('Bash / Shell'), ('PowerShell'),
    ('SQL'), ('PL/SQL'), ('T-SQL'), ('VHDL'), ('Verilog'),

    -- Développement front end
    ('HTML5'), ('CSS3'), ('Sass'), ('Tailwind CSS'), ('React'), ('Angular'),
    ('Vue.js'), ('Svelte'), ('Next.js'), ('Nuxt.js'), ('Redux'), ('Webpack'),
    ('Vite'), ('jQuery'), ('Bootstrap'), ('Web Components'), ('WebAssembly'),
    ('Accessibilité web (WCAG)'), ('Design responsive'),
    ('Progressive Web Apps'),

    -- Développement back end
    ('Spring'), ('Spring Boot'), ('Jakarta EE'), ('Hibernate'), ('JPA'),
    ('.NET Core'), ('ASP.NET'), ('Django'), ('Flask'), ('FastAPI'),
    ('Node.js'), ('Express.js'), ('NestJS'), ('Ruby on Rails'), ('Laravel'),
    ('Symfony'), ('Quarkus'), ('Micronaut'), ('gRPC'), ('GraphQL'),
    ('API REST'), ('SOAP'), ('WebSocket'), ('Conception d''API'),

    -- Bases de données
    ('PostgreSQL'), ('MySQL'), ('MariaDB'), ('Oracle Database'),
    ('SQL Server'), ('SQLite'), ('MongoDB'), ('Cassandra'), ('Redis'),
    ('Elasticsearch'), ('Neo4j'), ('DynamoDB'), ('Couchbase'), ('InfluxDB'),
    ('ClickHouse'), ('Firebase'), ('Modélisation de données'),
    ('Optimisation de requêtes'), ('Entrepôt de données'),

    -- Cloud, DevOps et systèmes
    ('Docker'), ('Kubernetes'), ('Terraform'), ('Ansible'), ('Jenkins'),
    ('GitLab CI'), ('GitHub Actions'), ('CircleCI'), ('AWS'), ('Azure'),
    ('Google Cloud Platform'), ('OpenShift'), ('Helm'), ('Prometheus'),
    ('Grafana'), ('Suite ELK'), ('Nginx'), ('Apache HTTP Server'),
    ('Administration Linux'), ('Windows Server'), ('CI/CD'),
    ('Infrastructure as Code'), ('Site Reliability Engineering'),
    ('Observabilité'), ('Répartition de charge'), ('Virtualisation'),

    -- Fondamentaux informatiques
    ('Algorithmique'), ('Structures de données'), ('Analyse de complexité'),
    ('Systèmes d''exploitation'), ('Réseaux informatiques'), ('Compilation'),
    ('Systèmes distribués'), ('Programmation concurrente'),
    ('Programmation parallèle'), ('Architecture des ordinateurs'),
    ('Méthodes formelles'), ('Théorie des automates'), ('Théorie des graphes'),
    ('Méthodes numériques'), ('Cryptographie'), ('Théorie de l''information'),
    ('Calcul haute performance'), ('Calcul scientifique'),

    -- Données et intelligence artificielle
    ('Machine Learning'), ('Deep Learning'),
    ('Traitement automatique du langage naturel'), ('Vision par ordinateur'),
    ('Apprentissage par renforcement'), ('Réseaux de neurones'),
    ('TensorFlow'), ('PyTorch'), ('scikit-learn'), ('Keras'), ('Pandas'),
    ('NumPy'), ('Analyse de données'), ('Visualisation de données'),
    ('Statistiques'), ('Big Data'), ('Apache Spark'), ('Hadoop'),
    ('Apache Kafka'), ('ETL'), ('Data Engineering'), ('MLOps'),
    ('IA générative et LLM'), ('Systèmes de recommandation'),
    ('Séries temporelles'), ('Business Intelligence'),

    -- Sécurité
    ('Cybersécurité'), ('Tests d''intrusion'), ('Sécurité applicative'),
    ('Sécurité réseau'), ('Protocoles cryptographiques'), ('OAuth2 / OIDC'),
    ('Gestion des identités'), ('Audit de sécurité'),
    ('Réponse à incident'), ('Rétro-ingénierie'), ('Analyse de malware'),
    ('Investigation numérique'), ('Zero Trust'), ('OWASP'),

    -- Mobile
    ('Android'), ('iOS'), ('React Native'), ('Flutter'), ('Xamarin'),
    ('Ionic'), ('UX mobile'),

    -- Domaines spécialisés
    ('Développement de jeux vidéo'), ('Unity'), ('Unreal Engine'),
    ('Infographie'), ('Modélisation 3D'), ('Réalité augmentée / virtuelle'),
    ('Systèmes embarqués'), ('Internet des objets'), ('Robotique'),
    ('Systèmes temps réel'), ('Firmware'), ('FPGA'),
    ('Traitement du signal'), ('Blockchain'), ('Smart contracts'),
    ('Informatique quantique'), ('Bio-informatique'),
    ('Systèmes d''information géographique'),

    -- Pratiques et qualité logicielle
    ('Agile / Scrum'), ('Kanban'), ('SAFe'), ('Test Driven Development'),
    ('Tests unitaires'), ('Tests d''intégration'), ('Tests end to end'),
    ('JUnit'), ('Selenium'), ('Cypress'), ('Jest'), ('Playwright'),
    ('Architecture logicielle'), ('Microservices'),
    ('Architecture événementielle'), ('Domain Driven Design'),
    ('Patrons de conception'), ('Clean code'), ('Revue de code'),
    ('Refactoring'), ('UML'), ('Documentation technique'),
    ('Optimisation des performances'), ('Débogage'),
    ('Conception de systèmes'),

    -- Outils
    ('Git'), ('Jira'), ('Confluence'), ('Figma'), ('Postman'), ('Maven'),
    ('Gradle'), ('npm'), ('IntelliJ IDEA'), ('Visual Studio Code'),
    ('Eclipse')
) AS t(libelle)
WHERE c.libelle = 'Compétences techniques';

-- ---------------------------------------------------------------------------
-- Niveau d'expérience (a trait, never an offer column)
-- ---------------------------------------------------------------------------

INSERT INTO trait (id_categorie, libelle)
SELECT c.id_categorie, t.libelle
FROM categorie_trait c
CROSS JOIN (VALUES
    ('Stage'),
    ('Alternance'),
    ('Débutant (moins de 1 an)'),
    ('Junior (1 à 3 ans)'),
    ('Confirmé (3 à 5 ans)'),
    ('Senior (5 à 8 ans)'),
    ('Expert (plus de 8 ans)'),
    ('Lead technique'),
    ('Architecte'),
    ('Manager d''équipe')
) AS t(libelle)
WHERE c.libelle = 'Niveau d''expérience';

-- ---------------------------------------------------------------------------
-- Langues (a trait, never an offer column)
-- ---------------------------------------------------------------------------

INSERT INTO trait (id_categorie, libelle)
SELECT c.id_categorie, t.libelle
FROM categorie_trait c
CROSS JOIN (VALUES
    ('Français'), ('Anglais'), ('Arabe'), ('Amazigh'), ('Espagnol'),
    ('Allemand'), ('Italien'), ('Portugais'), ('Néerlandais'), ('Russe'),
    ('Chinois mandarin'), ('Japonais'), ('Coréen'), ('Turc'), ('Polonais'),
    ('Suédois'), ('Norvégien'), ('Danois'), ('Grec'), ('Hébreu'),
    ('Hindi'), ('Ourdou'), ('Persan'), ('Vietnamien'), ('Thaï'),
    ('Indonésien'), ('Roumain'), ('Tchèque'), ('Hongrois'), ('Ukrainien')
) AS t(libelle)
WHERE c.libelle = 'Langues';

-- ---------------------------------------------------------------------------
-- Atouts (plus criteria: never gate visibility, used for ranking)
-- ---------------------------------------------------------------------------

INSERT INTO trait (id_categorie, libelle)
SELECT c.id_categorie, t.libelle
FROM categorie_trait c
CROSS JOIN (VALUES
    -- Certifications
    ('Certification AWS'), ('Certification Azure'),
    ('Certification Google Cloud'), ('Certification Kubernetes (CKA)'),
    ('Certification Scrum Master'), ('Certification PMP'),
    ('Certification ITIL'), ('Certification TOGAF'),
    ('Certification CISSP'), ('Certification CEH'),
    ('Certification Cisco CCNA'), ('Certification Oracle Java'),
    ('Certification Microsoft'), ('Certification Red Hat'),

    -- Disponibilité et mobilité
    ('Disponibilité immédiate'), ('Mobilité géographique'),
    ('Mobilité internationale'), ('Permis de conduire'),
    ('Ouverture au télétravail'), ('Ouverture aux déplacements fréquents'),

    -- Parcours
    ('Expérience internationale'), ('Expérience en management d''équipe'),
    ('Expérience en startup'), ('Expérience en grand groupe'),
    ('Expérience en ESN'), ('Expérience en conseil'),
    ('Contributions open source'), ('Publications scientifiques'),
    ('Brevets déposés'), ('Participation à des conférences'),
    ('Mentorat'), ('Enseignement'),

    -- Savoir être
    ('Communication'), ('Esprit d''équipe'), ('Autonomie'),
    ('Capacité d''adaptation'), ('Résolution de problèmes'),
    ('Esprit d''analyse'), ('Créativité'), ('Leadership'),
    ('Gestion du stress'), ('Rigueur'), ('Curiosité technique'),
    ('Sens du service client'), ('Négociation'), ('Gestion de projet'),
    ('Rédaction technique'), ('Veille technologique'),
    ('Prise de parole en public'), ('Esprit d''initiative')
) AS t(libelle)
WHERE c.libelle = 'Atouts';
