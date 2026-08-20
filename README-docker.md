# Running TeLTP with Docker

Runs the whole platform — MySQL, the Spring Boot backend, and the Angular
frontend (served by nginx) — with one command. No local Java, Node, Maven or
MySQL install required; everything builds inside containers.

## Layout

Place these three items together in one folder (e.g. `C:\Users\USER\My Drive`):

```
My Drive/
  docker-compose.yml
  .env
  teltp-backend/     (your backend project, now containing a Dockerfile)
  teltp-frontend/    (your frontend project, now containing a Dockerfile + nginx.conf)
```

## Run

From that folder:

```bash
docker compose up --build
```

First build downloads Maven and npm dependencies, so it takes a few minutes.
Subsequent starts are fast. When it's up:

- **App (use this):** http://localhost:9090
- Swagger UI (via the app origin): http://localhost:9090/api/swagger-ui.html
- (The backend is not published to a host port by default; see docker-compose.yml to expose it.)

The frontend calls `/api` on its own origin and nginx reverse-proxies that to
the backend, so there is no CORS concern in this setup.

Stop with `Ctrl+C`, or run detached with `docker compose up --build -d` and stop
with `docker compose down`.

## Data

MySQL data persists in the `teltp-db-data` volume across restarts. Flyway applies
V1–V4 (schema, roles/categories, demo courses, demo lessons) on first backend start.

To wipe everything and start clean:

```bash
docker compose down -v
```

## Make yourself an admin (one time)

Role assignment is admin-gated, so seed the first admin directly in the DB.
Register a user in the app first, then:

```bash
docker compose exec db mysql -uteltp -pteltp teltp -e "INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r WHERE u.username='YOUR_USERNAME' AND r.name='ADMIN';"
```

Log out and back in so the new role is in your token.

## Notes

- Change `JWT_SECRET` (and the DB passwords) in `.env` before any real deployment.
  `JWT_SECRET` must be at least 64 characters.
- Ports 8081, 8080 and 3306's internal use can be changed in `docker-compose.yml`
  if they clash with something on your machine.
