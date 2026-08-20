# TeLTP — TIRDO e-Learning & Training Platform

National Industrial Skills and Innovation Training Hub for the Tanzania Industrial
Research and Development Organization (TIRDO). A full-stack platform: a Spring Boot 3
/ Java 21 backend, an Angular 20 frontend, and MySQL — orchestrated with Docker Compose.

## Stack

- **Backend** — Spring Boot 3.3, Java 21, Flyway-managed MySQL schema, JWT auth + RBAC.
- **Frontend** — Angular 20 (standalone components, signals, Angular Material), served by nginx.
- **Database** — MySQL 8.4.

## Quick start (Docker)

Requires Docker Desktop.

```bash
cp .env.example .env          # then edit secrets (PowerShell: Copy-Item .env.example .env)
docker compose up --build     # or: .\run.ps1 start   (Windows helper)
```

Then open the app at **http://localhost:9090**.
Swagger API docs: http://localhost:9090/api/swagger-ui.html

See `README-docker.md` for details (admin bootstrap, resetting data, ports).

## Layout

```
Teltp/
  docker-compose.yml     three services: db, backend, frontend
  .env.example           copy to .env and fill in
  run.ps1                Windows task runner (start / stop / logs / admin / reset)
  teltp-backend/         Spring Boot API  (see teltp-backend/README.md)
  teltp-frontend/        Angular SPA      (see teltp-frontend/README.md)
```

## What's implemented

Auth & RBAC, course catalogue, enrolment, the learner course-player with persisted
per-lesson progress and course completion, plus backend support for assessments,
certificates (issue + public verification), scheduling, corporate contracts, billing,
and reporting.

## Security note

`.env` (real secrets) is gitignored. Never commit real credentials. Change
`JWT_SECRET` and the database passwords before any real deployment.
