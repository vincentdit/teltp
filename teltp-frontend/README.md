# TeLTP Frontend — Angular 20

Angular 20 single-page app for the TIRDO e-Learning & Training Platform, talking to the
Spring Boot backend. Standalone components, signals, Angular Material (M3), functional
HTTP interceptors and route guards.

## Run

```bash
npm install
npm start          # ng serve on http://localhost:4200
```

The dev build points the API at `http://localhost:8080/api` (see
`src/environments/environment.development.ts`). Start the backend first. CORS: until the
backend enables CORS for the dev origin, serve the frontend behind the same origin or add a
proxy (`proxy.conf.json`) — see "CORS" below.

## What's implemented

- **Auth** — login + registration, JWT stored client-side, session exposed as signals.
- **Interceptors** — `authInterceptor` attaches the Bearer token; `errorInterceptor` surfaces
  backend `ApiResponse.message` errors and redirects to `/login` on 401.
- **Guards** — `authGuard` (any authenticated user) and `roleGuard` (route `data.roles`).
- **Public catalogue** — paginated published courses + course detail with self-enrolment.
- **Student** — "My learning" dashboard listing enrolments.
- **Admin/instructor** — administration home + course management (create + publish/archive,
  exercising the publish state machine).

Every screen consumes the backend `ApiResponse<T>` / `PageResponse<T>` envelopes through
`ApiService`, so adding a module is: model → service method → component, following the same shape.

## Architecture

```
src/app/
  core/        models, services (api, auth, token, catalog, enrollment),
               interceptors (auth, error), guards (auth, role)
  layout/      shell (toolbar, role-aware nav, footer)
  features/    auth/ catalog/ dashboard/ admin/   (lazy-loaded routes)
```

## CORS (dev)

Easiest path: add `proxy.conf.json` at the project root:

```json
{ "/api": { "target": "http://localhost:8080", "secure": false } }
```

then run `ng serve --proxy-config proxy.conf.json` and set
`apiBaseUrl: '/api'` in `environment.development.ts`. Alternatively enable CORS in the backend
`SecurityConfig` for `http://localhost:4200`.

## Next slices (same pattern)

Curriculum builder (modules/lessons), assessments & attempts, certificate issue/verify,
schedule & webinars, billing/invoices with GePG, organizations, and reporting dashboards.
Each maps to an existing backend module and a `core/services/*.service.ts` method.
