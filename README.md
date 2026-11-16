# Risk Register

A small full-stack Risk Register: create risks, score them, attach mitigations, and see everything on a severity-coloured dashboard.

- **Backend:** Java 21, Spring Boot 3.5, Spring Data JPA, Flyway, PostgreSQL
- **Frontend:** React 19, TypeScript, Vite, TanStack Query, React Router, react-hook-form + zod
- Design doc: [docs/LLD.md](docs/LLD.md)

## Run it locally (about 5 minutes)

**Prerequisites**

| Tool | Version | Check |
|---|---|---|
| Java (JDK) | 21+ | `java -version` |
| Node.js | 20+ | `node -v` |
| PostgreSQL | any recent, running locally | `pg_isready` |

Maven is **not** required; the repo ships the Maven wrapper (`./mvnw`).

```bash
# 1. Database (once). Creates an empty database; Flyway builds the tables on first start.
createdb riskregister

# 2. Backend on http://localhost:8080
cd backend
./mvnw spring-boot:run

# 3. Frontend (in a second terminal)
cd frontend
npm install
npm run dev
```

Open the URL Vite prints (normally http://localhost:5173). The dev server proxies `/api` to the backend on port 8080, so there is nothing else to configure.

**Database settings.** By default the backend connects to `jdbc:postgresql://localhost:5432/riskregister` as your OS user with no password (the usual Homebrew / local-install setup). If yours differs, set environment variables before step 2:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/riskregister
export DB_USER=postgres
export DB_PASSWORD=secret
```

**Troubleshooting**

- `database "riskregister" does not exist`: run step 1.
- `password authentication failed` / `role ... does not exist`: set `DB_USER` and `DB_PASSWORD` as above.
- `Port 8080 was already in use`: stop the other process, or run `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` and set the proxy target in `frontend/vite.config.ts`.
- Dashboard shows "Cannot reach the server": the backend is not running yet.

## Tests

```bash
cd backend  && ./mvnw test     # 46 tests: scoring units, service rules, API integration
cd frontend && npm test        # 12 tests (Vitest)
```

No database is needed to run the tests. The backend integration tests use in-memory H2 in PostgreSQL mode. The trade-off: H2 can hide Postgres-specific behaviour, so the migration sticks to portable SQL, and the app was also run against real Postgres (Hibernate `ddl-auto=validate` checks the entities against the Flyway schema at startup).

## Scoring

**Inherent = likelihood × impact** (1–25).

**Residual** starts from the inherent score and applies each mitigation:

1. A mitigation with effectiveness `e` (1–5) removes `15% × e` of the risk (15% to 75%).
2. Mitigations combine multiplicatively: `remaining = Π (1 − 0.15·eᵢ)`. Each control reduces what is left, so stacking never exceeds 100%.
3. Total reduction is capped at 90% (`remaining ≥ 0.10`), so no number of controls makes a risk disappear.
4. `residual = max(1, round_half_up(inherent × remaining))`

| Inherent | Mitigations | Residual |
|---|---|---|
| 20 | none | 20 (equals inherent) |
| 20 | one at 5 | 5 |
| 20 | two at 3 | 6 |
| 25 | three at 5 | 3 (cap applies) |
| 1 | one at 5 | 1 (floor) |

Why this shape: additive reduction can go past 100%, and averaging effectiveness would mean a weak second control *raises* the residual. Multiplication is monotonic (a new control never makes things worse), order-independent, and matches how layered controls work in practice. The weights (15% per point, 90% cap) are my judgement call and are constants in one class; swapping the whole formula means implementing `ResidualScoreCalculator`.

Severity bands apply to both scores: Low 1–5, Medium 6–12, High 13–19, Critical 20–25.

Scores are **computed on read, never stored**, so they cannot go stale when mitigations change.

## Business rule: closing a risk

A risk cannot be set to **Closed** while it has zero mitigations. The API returns `409 Conflict` with a message.

Reasoning: in a compliance product, "Closed" tells an auditor the risk was treated. A closed risk with no controls looks like a risk that was hidden. The rule is applied consistently:

- creating a risk directly as Closed is rejected (it has no mitigations by definition)
- deleting the **last** mitigation of a Closed risk is rejected too, otherwise the rule is bypassed by deleting afterwards

I chose 409 rather than 400 because the payload is valid; it's the resource's current state that forbids it.

## API

| Method | Path |
|---|---|
| POST / GET | `/api/risks` (filters `category`, `status`; `sort=residualScore,desc\|asc\|createdAt,desc`) |
| GET / PUT / DELETE | `/api/risks/{id}` |
| POST | `/api/risks/{riskId}/mitigations` |
| PUT / DELETE | `/api/risks/{riskId}/mitigations/{id}` |

Errors are RFC 7807 `application/problem+json`. Validation failures are `400` with `errors: [{field, message}]`. Missing resources are `404`; the closing rule is `409`. Non-integer values such as `3.5` for likelihood are rejected rather than truncated.

## Assumptions and trade-offs

- Single implicit organisation, no auth.
- The list is not paginated and the residual sort happens in memory, because residual is derived. At larger scale I would paginate and persist a denormalised score kept in sync in the same transaction.
- Filter and sort state is kept in the URL, so views are shareable.
- Severity bands come from the server. The form's live inherent preview is the only client-side calculation, and is covered by a test.
- Last write wins on `PUT` (no optimistic locking).
- Mitigation edits do not bump the parent risk's `updatedAt`.
- Mitigation edit is in the API but not the UI (the brief only requires adding).
- Frontend tooling is pinned to Vite 6 / Vitest 3 / jsdom 26 so it also works on Node 20.

## With more time

- Pagination and server-side sorting, optimistic locking, and a score-history/audit trail.
- A "risk accepted" or "closed with justification" path, since some risks are legitimately closed without a control.
- Mitigation edit UI and optimistic updates.
- Testcontainers Postgres for the integration tests, plus component tests for the detail page.
- Stretch goals: framework mapping (NIST CSF / SOC 2) and next-review dates with an overdue indicator.
