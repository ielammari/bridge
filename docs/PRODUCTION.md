# Running Bridge in production

Production is the clean version of Bridge: no demonstration data, a schema created in one step with the trait catalogue, and one first recruiter taken from the environment. It needs PostgreSQL 16, a place on disk for CVs, and HTTPS in front of it.

There are two ways to run it: [as containers](#as-containers), with everything in Docker, or [as a jar](#as-a-jar) on a machine that already has Java and PostgreSQL.

## Secrets

| Variable | Purpose |
| --- | --- |
| `BRIDGE_JWT_SECRET` | Signs the session tokens. At least 32 characters; `openssl rand -hex 32` gives 64. Changing it signs everyone out. |
| `BRIDGE_DB_PASSWORD` | The database password. |
| `BRIDGE_ADMIN_EMAIL`, `BRIDGE_ADMIN_PASSWORD` | The first recruiter, created at the first start. The password follows the policy and is replaced at first sign in. |
| `BRIDGE_GOOGLE_CLIENT_ID` | Optional. Enables Google sign in for candidates. |

They live in a `.env` file beside the application, readable by its user only (`chmod 600 .env`). Once the first recruiter exists, the two `BRIDGE_ADMIN_` lines are no longer read and can be removed. Further recruiter and expert accounts are created from **Paramètres**.

## As containers

`docker-compose.prod.yml` builds the image from `Dockerfile` and runs it with its own PostgreSQL:

- **`db`** is PostgreSQL 16. Its data lives in the `database` volume, and it is reachable only from the application, never from the host.
- **`app`** is the application, built from the sources in two stages: a JDK stage compiles the prod build, and a smaller Java runtime image runs it as an unprivileged user. It waits for the database to be healthy, reads its settings from `.env`, keeps CVs in the `cv` volume, and listens on `127.0.0.1:8080` for the reverse proxy.

```bash
git clone https://github.com/ielammari/bridge.git
cd bridge
cp .env.example .env              # then fill in the secrets above
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml logs -f app
```

The log shows two migrations applied and the first recruiter created. Compose refuses to start while `BRIDGE_JWT_SECRET` or `BRIDGE_DB_PASSWORD` is empty.

| Task | Command |
| --- | --- |
| Stop | `docker compose -f docker-compose.prod.yml down` |
| Start again | `docker compose -f docker-compose.prod.yml up -d` |
| Delete everything, data included | `docker compose -f docker-compose.prod.yml down -v` |

The stack is named `bridge-prod`, so it never collides with the development database.

## As a jar

Requirements: a Java 21 runtime and a PostgreSQL 16 database with its own user.

Build on any machine with the sources:

```bash
./mvnw package -Pprod -DskipTests
```

Copy `target/prod/bridge-prod.jar` to the server, for example to `/opt/bridge`, and write `/opt/bridge/.env`:

```properties
BRIDGE_DB_URL=jdbc:postgresql://localhost:5432/bridge
BRIDGE_DB_USER=bridge
BRIDGE_DB_PASSWORD=<database password>
BRIDGE_JWT_SECRET=<64 hexadecimal characters>
BRIDGE_ADMIN_EMAIL=<first recruiter>
BRIDGE_ADMIN_PASSWORD=<a strong password>
```

The jar defaults to the prod profile and reads `.env` from its working directory. CVs go to `storage/prod/cv` under that directory, or wherever `BRIDGE_CV_DIR` points.

To run it as a service, create a `bridge` system user that owns `/opt/bridge`, and `/etc/systemd/system/bridge.service`:

```ini
[Unit]
Description=Bridge
After=network.target postgresql.service

[Service]
User=bridge
WorkingDirectory=/opt/bridge
ExecStart=/usr/bin/java -jar /opt/bridge/bridge-prod.jar --server.address=127.0.0.1
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable --now bridge
journalctl -u bridge -f
```

## HTTPS

The application speaks plain HTTP on `127.0.0.1:8080`, and a reverse proxy on the same machine serves it over HTTPS. With [Caddy](https://caddyserver.com/), which obtains and renews the certificate itself, the whole configuration is:

```
bridge.example.com {
    reverse_proxy 127.0.0.1:8080
}
```

With nginx and a certificate from Certbot:

```nginx
server {
    listen 443 ssl;
    server_name bridge.example.com;
    ssl_certificate     /etc/letsencrypt/live/bridge.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/bridge.example.com/privkey.pem;
    client_max_body_size 6m;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

`client_max_body_size` lets a 5 MB CV through; nginx stops at 1 MB by default.

## Google sign in

Add the production address, for example `https://bridge.example.com`, to the **Authorized JavaScript origins** of the OAuth client, set `BRIDGE_GOOGLE_CLIENT_ID`, and restart. Once the consent screen is published, any Google account can sign in, not only test users.

## Backups

Two things hold the data: the database and the CV files. Back up both, together.

As containers:

```bash
docker compose -f docker-compose.prod.yml exec -T db pg_dump -U bridge bridge > bridge-$(date +%F).sql
docker run --rm -v bridge-prod_cv:/cv -v "$PWD":/backup alpine tar czf /backup/cv-$(date +%F).tgz -C /cv .
```

As a jar:

```bash
pg_dump -U bridge bridge > bridge-$(date +%F).sql
tar czf cv-$(date +%F).tgz -C /opt/bridge/storage/prod/cv .
```

To restore, load the dump into an empty database with `psql -U bridge bridge < bridge-<date>.sql` (through `docker compose ... exec -T db` for containers), and extract the archive back into the CV directory or volume, before starting the application.

## Upgrading

Back up first. Then, as containers:

```bash
git pull
docker compose -f docker-compose.prod.yml up -d --build
```

As a jar, build the new one, replace the file and `sudo systemctl restart bridge`.

Database changes apply themselves at start. For developers: every schema change is written twice, as the next migration in `db/migration` for the demo and the next one in `db/prod` for production.
