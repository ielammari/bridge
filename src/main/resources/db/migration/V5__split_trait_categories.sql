-- Splits the two umbrella categories, which held 255 and 50 traits, into
-- browsable ones. Traits keep their identifiers, so candidate profiles and
-- offer requirements are untouched.

-- Gives categories an explicit display order. One added later without a rank
-- lands at the end.
ALTER TABLE categorie_trait ADD COLUMN ordre SMALLINT NOT NULL DEFAULT 999;

INSERT INTO categorie_trait (libelle, ordre) VALUES
    ('Langages de programmation', 10),
    ('Développement front end', 20),
    ('Développement back end', 30),
    ('Bases de données', 40),
    ('Cloud, DevOps et systèmes', 50),
    ('Fondamentaux informatiques', 60),
    ('Données et intelligence artificielle', 70),
    ('Sécurité', 80),
    ('Mobile', 90),
    ('Domaines spécialisés', 100),
    ('Pratiques et qualité logicielle', 110),
    ('Outils', 120),
    ('Certifications', 150),
    ('Disponibilité et mobilité', 160),
    ('Parcours et expérience', 170),
    ('Savoir être', 180);

UPDATE categorie_trait SET ordre = 130 WHERE libelle = 'Niveau d''expérience';
UPDATE categorie_trait SET ordre = 140 WHERE libelle = 'Langues';

-- Langages de programmation (47)
UPDATE trait SET id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Langages de programmation')
WHERE id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Compétences techniques')
  AND libelle IN (
    'Java', 'Python', 'JavaScript', 'TypeScript', 'C', 'C++', 'C#', 'Go',
    'Rust', 'Kotlin', 'Swift', 'Ruby', 'PHP', 'Scala', 'Perl', 'R',
    'MATLAB', 'Dart', 'Elixir', 'Haskell', 'Clojure', 'F#', 'Groovy',
    'Lua', 'Julia', 'Objective-C', 'Visual Basic .NET', 'Assembleur',
    'COBOL', 'Fortran', 'Ada', 'Erlang', 'OCaml', 'Racket', 'Scheme',
    'Prolog', 'Solidity', 'Zig', 'Nim', 'Crystal', 'Bash / Shell',
    'PowerShell', 'SQL', 'PL/SQL', 'T-SQL', 'VHDL', 'Verilog'
  );

-- Développement front end (20)
UPDATE trait SET id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Développement front end')
WHERE id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Compétences techniques')
  AND libelle IN (
    'HTML5', 'CSS3', 'Sass', 'Tailwind CSS', 'React', 'Angular', 'Vue.js',
    'Svelte', 'Next.js', 'Nuxt.js', 'Redux', 'Webpack', 'Vite', 'jQuery',
    'Bootstrap', 'Web Components', 'WebAssembly',
    'Accessibilité web (WCAG)', 'Design responsive',
    'Progressive Web Apps'
  );

-- Développement back end (24)
UPDATE trait SET id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Développement back end')
WHERE id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Compétences techniques')
  AND libelle IN (
    'Spring', 'Spring Boot', 'Jakarta EE', 'Hibernate', 'JPA',
    '.NET Core', 'ASP.NET', 'Django', 'Flask', 'FastAPI', 'Node.js',
    'Express.js', 'NestJS', 'Ruby on Rails', 'Laravel', 'Symfony',
    'Quarkus', 'Micronaut', 'gRPC', 'GraphQL', 'API REST', 'SOAP',
    'WebSocket', 'Conception d''API'
  );

-- Bases de données (19)
UPDATE trait SET id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Bases de données')
WHERE id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Compétences techniques')
  AND libelle IN (
    'PostgreSQL', 'MySQL', 'MariaDB', 'Oracle Database', 'SQL Server',
    'SQLite', 'MongoDB', 'Cassandra', 'Redis', 'Elasticsearch', 'Neo4j',
    'DynamoDB', 'Couchbase', 'InfluxDB', 'ClickHouse', 'Firebase',
    'Modélisation de données', 'Optimisation de requêtes',
    'Entrepôt de données'
  );

-- Cloud, DevOps et systèmes (26)
UPDATE trait SET id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Cloud, DevOps et systèmes')
WHERE id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Compétences techniques')
  AND libelle IN (
    'Docker', 'Kubernetes', 'Terraform', 'Ansible', 'Jenkins',
    'GitLab CI', 'GitHub Actions', 'CircleCI', 'AWS', 'Azure',
    'Google Cloud Platform', 'OpenShift', 'Helm', 'Prometheus', 'Grafana',
    'Suite ELK', 'Nginx', 'Apache HTTP Server', 'Administration Linux',
    'Windows Server', 'CI/CD', 'Infrastructure as Code',
    'Site Reliability Engineering', 'Observabilité',
    'Répartition de charge', 'Virtualisation'
  );

-- Fondamentaux informatiques (18)
UPDATE trait SET id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Fondamentaux informatiques')
WHERE id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Compétences techniques')
  AND libelle IN (
    'Algorithmique', 'Structures de données', 'Analyse de complexité',
    'Systèmes d''exploitation', 'Réseaux informatiques', 'Compilation',
    'Systèmes distribués', 'Programmation concurrente',
    'Programmation parallèle', 'Architecture des ordinateurs',
    'Méthodes formelles', 'Théorie des automates', 'Théorie des graphes',
    'Méthodes numériques', 'Cryptographie', 'Théorie de l''information',
    'Calcul haute performance', 'Calcul scientifique'
  );

-- Données et intelligence artificielle (26)
UPDATE trait SET id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Données et intelligence artificielle')
WHERE id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Compétences techniques')
  AND libelle IN (
    'Machine Learning', 'Deep Learning',
    'Traitement automatique du langage naturel', 'Vision par ordinateur',
    'Apprentissage par renforcement', 'Réseaux de neurones', 'TensorFlow',
    'PyTorch', 'scikit-learn', 'Keras', 'Pandas', 'NumPy',
    'Analyse de données', 'Visualisation de données', 'Statistiques',
    'Big Data', 'Apache Spark', 'Hadoop', 'Apache Kafka', 'ETL',
    'Data Engineering', 'MLOps', 'IA générative et LLM',
    'Systèmes de recommandation', 'Séries temporelles',
    'Business Intelligence'
  );

-- Sécurité (14)
UPDATE trait SET id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Sécurité')
WHERE id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Compétences techniques')
  AND libelle IN (
    'Cybersécurité', 'Tests d''intrusion', 'Sécurité applicative',
    'Sécurité réseau', 'Protocoles cryptographiques', 'OAuth2 / OIDC',
    'Gestion des identités', 'Audit de sécurité', 'Réponse à incident',
    'Rétro-ingénierie', 'Analyse de malware', 'Investigation numérique',
    'Zero Trust', 'OWASP'
  );

-- Mobile (7)
UPDATE trait SET id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Mobile')
WHERE id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Compétences techniques')
  AND libelle IN (
    'Android', 'iOS', 'React Native', 'Flutter', 'Xamarin', 'Ionic',
    'UX mobile'
  );

-- Domaines spécialisés (18)
UPDATE trait SET id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Domaines spécialisés')
WHERE id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Compétences techniques')
  AND libelle IN (
    'Développement de jeux vidéo', 'Unity', 'Unreal Engine',
    'Infographie', 'Modélisation 3D', 'Réalité augmentée / virtuelle',
    'Systèmes embarqués', 'Internet des objets', 'Robotique',
    'Systèmes temps réel', 'Firmware', 'FPGA', 'Traitement du signal',
    'Blockchain', 'Smart contracts', 'Informatique quantique',
    'Bio-informatique', 'Systèmes d''information géographique'
  );

-- Pratiques et qualité logicielle (25)
UPDATE trait SET id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Pratiques et qualité logicielle')
WHERE id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Compétences techniques')
  AND libelle IN (
    'Agile / Scrum', 'Kanban', 'SAFe', 'Test Driven Development',
    'Tests unitaires', 'Tests d''intégration', 'Tests end to end',
    'JUnit', 'Selenium', 'Cypress', 'Jest', 'Playwright',
    'Architecture logicielle', 'Microservices',
    'Architecture événementielle', 'Domain Driven Design',
    'Patrons de conception', 'Clean code', 'Revue de code', 'Refactoring',
    'UML', 'Documentation technique', 'Optimisation des performances',
    'Débogage', 'Conception de systèmes'
  );

-- Outils (11)
UPDATE trait SET id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Outils')
WHERE id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Compétences techniques')
  AND libelle IN (
    'Git', 'Jira', 'Confluence', 'Figma', 'Postman', 'Maven', 'Gradle',
    'npm', 'IntelliJ IDEA', 'Visual Studio Code', 'Eclipse'
  );

-- Certifications (14)
UPDATE trait SET id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Certifications')
WHERE id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Atouts')
  AND libelle IN (
    'Certification AWS', 'Certification Azure',
    'Certification Google Cloud', 'Certification Kubernetes (CKA)',
    'Certification Scrum Master', 'Certification PMP',
    'Certification ITIL', 'Certification TOGAF', 'Certification CISSP',
    'Certification CEH', 'Certification Cisco CCNA',
    'Certification Oracle Java', 'Certification Microsoft',
    'Certification Red Hat'
  );

-- Disponibilité et mobilité (6)
UPDATE trait SET id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Disponibilité et mobilité')
WHERE id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Atouts')
  AND libelle IN (
    'Disponibilité immédiate', 'Mobilité géographique',
    'Mobilité internationale', 'Permis de conduire',
    'Ouverture au télétravail', 'Ouverture aux déplacements fréquents'
  );

-- Parcours et expérience (12)
UPDATE trait SET id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Parcours et expérience')
WHERE id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Atouts')
  AND libelle IN (
    'Expérience internationale', 'Expérience en management d''équipe',
    'Expérience en startup', 'Expérience en grand groupe',
    'Expérience en ESN', 'Expérience en conseil',
    'Contributions open source', 'Publications scientifiques',
    'Brevets déposés', 'Participation à des conférences', 'Mentorat',
    'Enseignement'
  );

-- Savoir être (18)
UPDATE trait SET id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Savoir être')
WHERE id_categorie =
        (SELECT id_categorie FROM categorie_trait WHERE libelle = 'Atouts')
  AND libelle IN (
    'Communication', 'Esprit d''équipe', 'Autonomie',
    'Capacité d''adaptation', 'Résolution de problèmes',
    'Esprit d''analyse', 'Créativité', 'Leadership', 'Gestion du stress',
    'Rigueur', 'Curiosité technique', 'Sens du service client',
    'Négociation', 'Gestion de projet', 'Rédaction technique',
    'Veille technologique', 'Prise de parole en public',
    'Esprit d''initiative'
  );

-- Both umbrellas are empty now. The foreign key is ON DELETE RESTRICT, so this
-- fails loudly if any trait was left behind.
DELETE FROM categorie_trait WHERE libelle IN ('Compétences techniques', 'Atouts');
