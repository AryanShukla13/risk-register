# Risk Register

A small full-stack Risk Register: create risks, score them, attach mitigations, and see everything on a severity-coloured dashboard.

- **Backend:** Java 21, Spring Boot 3.5, Spring Data JPA, Flyway, PostgreSQL
- **Frontend:** React 19, TypeScript, Vite, TanStack Query, React Router, react-hook-form + zod
- Design doc: [docs/LLD.md](docs/LLD.md)

## Run it locally (about 3 minutes)

Prerequisites: Java 21+, Maven 3.9+, Node 20+, PostgreSQL running locally.

```bash
# 1. Database (once)
createdb riskregister

# 2. Backend, on :8080 (Flyway creates the tables on first start)
cd backend
mvn spring-boot:run

# 3. Frontend, on :5173 (new terminal; /api is proxied to :8080)
cd frontend
npm install
npm run dev
```

Open http://localhost:5173.

Database settings default to `jdbc:postgresql://localhost:5432/riskregister` with your OS username and no password. Override with `DB_URL`, `DB_USER`, `DB_PASSWORD`.

## Tests

```bash
cd backend  && mvn test        # unit + integration tests
cd frontend && npm test        # Vitest
```

Backend integration tests run against in-memory H2 in PostgreSQL mode, so no database is needed to run them. The trade-off: H2 can hide Postgres-specific behaviour. The migration sticks to portable SQL for that reason, and I checked the app against real Postgres by hand (Hibernate `ddl-auto=validate` checks entities against the Flyway schema at startup).

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
