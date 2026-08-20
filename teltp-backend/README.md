# TeLTP Backend — TIRDO e-Learning & Training Platform

National Industrial Skills and Innovation Training Hub. Spring Boot 3.3.4 / Java 21 / MySQL 8,
Flyway-owned schema with JPA in `validate` mode. Modular monolith; Angular 20 frontend is a
deferred deliverable. Conventions inherited from the CIAP backend.

## Run

```bash
# requires MySQL 8 reachable; create an empty schema named `teltp`
export DB_HOST=localhost DB_USER=teltp DB_PASSWORD=teltp DB_NAME=teltp
export JWT_SECRET=$(openssl rand -base64 64)   # must be >= 512 bits for HS512
mvn spring-boot:run
```

Flyway applies `V1__init_schema.sql` then `V2__seed_data.sql` on first start (seeds the five roles
and the top-level training taxonomy). API base path is `/api`; Swagger UI at `/api/swagger-ui.html`.

## Modules (16)

| Module | Responsibility |
|---|---|
| `auth` | Users, roles (ADMIN/INSTRUCTOR/STUDENT/CORPORATE_CLIENT/FINANCE_OFFICER), JWT login/register/refresh |
| `organization` | First-class orgs (government/industrial/academic/development-partner) for B2B |
| `catalog` | Categories (hierarchical), courses (delivery mode + publish state machine), modules, lessons |
| `content` | Learning materials stored by reference (filesystem impl; S3 + SCORM are seams) |
| `enrollment` | Cohorts, self-enrolment, corporate bulk-assignment, waitlists |
| `progress` | Per-lesson completion + course-completion gate |
| `forum` | Per-course discussion threads and posts |
| `assessment` | Quizzes/exams, four question types, attempts, auto-grade MCQ + manual grading |
| `certification` | PDF certificates (OpenPDF) + QR (ZXing) + public verification + renewal |
| `schedule` | Unified `ScheduledEvent` (training sessions + webinars), registration, attendance |
| `corporate` | B2B training contracts with quotation workflow |
| `billing` | Pricing plans, invoices, payments, subscriptions; four charge models |
| `marketplace` | Digital products checked out through the billing engine |
| `notification` | In-app notifications fanned out to enabled channels |
| `reporting` | Read-model aggregates (dashboards deferred to Angular) |
| `audit` | Dedicated action trail, distinct from row provenance |

## Architecture conventions

- Every aggregate extends `BaseEntity` (id, uuid, audit provenance, `@Version`, soft-delete `deleted`).
  `Payment` and `AuditLog` deliberately carry only their own columns (append-only facts).
- Controllers return `ApiResponse<T>`; list endpoints return `PageResponse<T>`.
- Reference numbers `TELTP-{MODULE}-{YEAR}-{00001}` via a gapless DB sequence (`reference_sequence`).
- Cross-module resolution goes through `getEntity(uuid)` hooks; modules reference each other by uuid,
  not by hard entity coupling.
- Schema is Flyway-owned; JPA validates only. The API Gateway in the concept diagram is infrastructure
  (e.g. Spring Cloud Gateway / nginx), not application code in this repository.

## Integration seams (all disabled in v1, wired to flip on via `application.yml`)

- **Payments** — `GEPG`, `MOBILE_MONEY`, `BANK_TRANSFER` behind `PaymentMethod`. GePG ships as a stub that
  emits a pseudo control number; real SP integration + signing certificates slot into `GepgPaymentMethod`.
  The public callback `/billing/gepg/callback` is whitelisted in `SecurityConfig`.
- **Notifications** — `EmailNotificationChannel`, `SmsNotificationChannel` behind `NotificationChannel`.
- **Virtual classroom** — `MeetingProvisioner` (MANUAL default; ZOOM/TEAMS/JITSI seam). Links/recordings
  are stored by reference, not rebuilt.
- **Identity** — `NidaVerificationService` (NIDA national-ID verification seam; returns unverified, never fabricates).
- **Content storage** — `ContentStorage` (filesystem impl; S3 seam).
- **MFA** — `MfaService` seam (`teltp.security.mfa.enabled=false`).
- **Internal systems** — CIAP / LIMS / ERP / DMS / RMS integration documented in
  `integration/internal/InternalSystemsIntegration`, to be built once published contracts exist.

## Compliance touchpoints (Security Framework, §10)

- JWT auth + method-level RBAC (`@PreAuthorize`); TLS terminated at the gateway.
- Audit trail (`audit_logs`) for accountability.
- PDPA: `dataProcessingConsent` captured at registration; soft-delete supports erasure requests.
- MFA seam present for step-up authentication when required.

## One thing to verify against your MySQL

Under Hibernate `validate`, column types must match what the dialect expects. This schema uses `BIT`
for booleans and `DATETIME(6)` for `Instant`, matching the Hibernate 6 / MySQL 8 defaults (same as CIAP).
If your environment maps `boolean` to `TINYINT(1)` instead, adjust the `BIT` columns in `V1` accordingly —
this is the only type that varies across setups.
