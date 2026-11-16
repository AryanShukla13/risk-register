# Risk Register: Low-Level Design

Scope: exactly what the assignment asks for. Nothing beyond it (no auth, no stretch goals, no containers, no pagination).

---

## 1. System overview

```
┌───────────────────────┐   HTTP/JSON    ┌──────────────────────────────────┐   JDBC   ┌────────────┐
│ React + TS (Vite SPA) │ ─────────────▶ │ Spring Boot REST API (Java 21)   │ ───────▶ │ PostgreSQL │
│ /api proxied in dev   │ ◀───────────── │ Controller → Service → Repository│          └────────────┘
└───────────────────────┘                └──────────────────────────────────┘
```

Repo layout (monorepo):

```
risk-register/
├── backend/      Spring Boot, Maven
├── frontend/     Vite + React + TS
├── docs/LLD.md
└── README.md     setup, formula + rationale, assumptions, trade-offs, "with more time"
```

---

## 2. Key design decisions

| # | Decision | Reason |
|---|----------|--------|
| D1 | **Scores are derived, never stored.** Inherent and residual are computed on read. | No stale or inconsistent data. Changing a mitigation can never leave a wrong score in the DB. |
| D2 | **Scoring is a pure, framework-free class** (`RiskScoring`) with no Spring or JPA imports. | The assignment says this is the part that must be most correct. Pure functions are trivial to unit test exhaustively. |
| D3 | **Residual formula behind an interface** (`ResidualScoreCalculator`, Strategy pattern). | The formula is deliberately open. The interface isolates it so it is swappable and testable on its own. |
| D4 | **Server is the source of truth for severity bands.** API responses include `inherentSeverity` and `residualSeverity`. | The frontend never re-derives bands for stored data. The form's live preview is the only client-side calculation. |
| D5 | **Closing a risk requires at least one mitigation.** Enforced on risk create/update and on mitigation delete. | Compliance products must show evidence of treatment. See §6. |
| D6 | **Separate DTOs from entities.** | Entities never leak to the API. Validation lives on request DTOs. |
| D7 | **Errors use RFC 7807 `ProblemDetail`** through one `@RestControllerAdvice`. | Consistent, clear 4xx bodies with per-field errors. |
| D8 | **Flyway migrations, `ddl-auto=validate`.** | The schema is explicit and versioned, with DB-level CHECK constraints as a second line of defence. |

---

## 3. Backend design (Spring Boot 3, Java 21, Maven)

### 3.1 Patterns used

- **Layered architecture:** Controller (HTTP only) → Service (use-cases, transactions, business rules) → Repository (persistence).
- **Aggregate root:** `Risk` owns its `Mitigation`s. All mitigation operations go through a risk id, which also enforces the URL hierarchy.
- **Repository pattern** with Spring Data JPA. Filtering uses `Specification` so optional `category` and `status` combine cleanly.
- **Strategy:** `ResidualScoreCalculator`, with one default implementation.
- **DTO + mapper:** a small hand-written `RiskMapper` (no MapStruct, to keep dependencies minimal).
- **Domain exception → HTTP mapping** in a single advice class.
- **Constructor injection only.**

### 3.2 Package structure

```
com.riskregister
├── RiskRegisterApplication
├── risk
│   ├── Risk                      (@Entity)
│   ├── RiskCategory              (enum: OPERATIONAL, FINANCIAL, COMPLIANCE, SECURITY, STRATEGIC)
│   ├── RiskStatus                (enum: OPEN, MITIGATING, CLOSED)
│   ├── RiskRepository            (JpaRepository + JpaSpecificationExecutor)
│   ├── RiskSpecifications        (byCategory, byStatus)
│   ├── RiskService
│   ├── RiskController
│   ├── RiskMapper
│   └── dto: RiskRequest, RiskResponse, RiskSummaryResponse
├── mitigation
│   ├── Mitigation                (@Entity)
│   ├── MitigationRepository
│   ├── MitigationService
│   ├── MitigationController
│   └── dto: MitigationRequest, MitigationResponse
├── scoring                       (pure Java, no framework imports)
│   ├── RiskScoring               (inherent(l, i), bands)
│   ├── ResidualScoreCalculator   (interface)
│   ├── DiminishingReturnsResidualCalculator (default impl)
│   └── SeverityBand              (enum LOW, MEDIUM, HIGH, CRITICAL; static of(int score))
└── common
    ├── error: GlobalExceptionHandler, NotFoundException, BusinessRuleException
    └── config: JacksonConfig (disable ACCEPT_FLOAT_AS_INT)
```

### 3.3 Core class responsibilities

```java
// scoring: pure
interface ResidualScoreCalculator {
    int residual(int inherentScore, List<Integer> mitigationEffectiveness);
}

enum SeverityBand {
    LOW(1,5), MEDIUM(6,12), HIGH(13,19), CRITICAL(20,25);
    static SeverityBand of(int score);   // throws IllegalArgumentException if outside 1..25
}

final class RiskScoring {
    static int inherent(int likelihood, int impact);  // l * i
}
```

```java
// RiskService (@Transactional)
RiskResponse create(RiskRequest req);                       // applies closed rule
RiskResponse get(long id);                                  // 404 if missing
List<RiskSummaryResponse> list(RiskCategory c, RiskStatus s, SortOrder sort);
RiskResponse update(long id, RiskRequest req);              // applies closed rule
void delete(long id);                                       // mitigations cascade

// MitigationService (@Transactional)
MitigationResponse create(long riskId, MitigationRequest req);
MitigationResponse update(long riskId, long id, MitigationRequest req);
void delete(long riskId, long id);                          // refuses if it would orphan a CLOSED risk
```

The shared closed-rule check lives in one place, `RiskService.assertCanClose(Risk, mitigationCount)`. Mitigation deletion uses the same rule.

### 3.4 Listing and sorting

- Filter by `category` and `status` through `Specification`.
- Load mitigations with `@EntityGraph(attributePaths = "mitigations")`, which avoids N+1 queries.
- Compute residual in the service, then sort in memory by `residualScore` descending. Ties break on `inherentScore` descending, then `id`.
- Default sort is `createdAt` descending. The `sort` parameter accepts `residualScore,desc` and `residualScore,asc`.
- Trade-off: in-memory sorting is fine at this scale. D1 is why we don't store residual. More time would mean pagination and a persisted score.

### 3.5 REST API contract

Base path `/api`. JSON only.

| Method | Path | Notes | Success |
|--------|------|-------|---------|
| POST | `/risks` | create | 201 + `Location` |
| GET | `/risks?category=&status=&sort=residualScore,desc` | list (summary rows) | 200 |
| GET | `/risks/{id}` | detail, includes mitigations | 200 |
| PUT | `/risks/{id}` | full replace | 200 |
| DELETE | `/risks/{id}` | cascades to mitigations | 204 |
| POST | `/risks/{riskId}/mitigations` | create | 201 |
| PUT | `/risks/{riskId}/mitigations/{id}` | full replace | 200 |
| DELETE | `/risks/{riskId}/mitigations/{id}` | | 204 |

**RiskRequest**
```json
{ "title": "string 1..200 required", "description": "string ≤2000, optional",
  "category": "SECURITY", "owner": "string 1..100 required",
  "likelihood": 4, "impact": 5, "status": "OPEN" }
```

**RiskResponse (detail)**
```json
{ "id": 1, "title": "...", "description": "...", "category": "SECURITY", "owner": "...",
  "likelihood": 4, "impact": 5, "status": "MITIGATING",
  "inherentScore": 20, "inherentSeverity": "CRITICAL",
  "residualScore": 5,  "residualSeverity": "LOW",
  "mitigations": [ { "id": 9, "riskId": 1, "description": "...", "effectiveness": 5, "createdAt": "..." } ],
  "createdAt": "...", "updatedAt": "..." }
```

**RiskSummaryResponse (list row):** the same fields minus `description` and `mitigations`, plus `mitigationCount`.

**MitigationRequest:** `{ "description": "1..1000 required", "effectiveness": 1..5 }`

### 3.6 Validation and error model

- Bean Validation on request DTOs: `@NotNull @Min(1) @Max(5) Integer`. Wrapper types are used so a missing value yields a validation error, not a silent `0`.
- Jackson `ACCEPT_FLOAT_AS_INT` is disabled, so `3.5` is rejected and not silently truncated to `3`.
- Unknown enum values and malformed JSON map to 400 with a message listing the allowed values.

| Condition | HTTP | Exception |
|-----------|------|-----------|
| Invalid body, enum, or type | 400 | `MethodArgumentNotValidException`, `HttpMessageNotReadableException` |
| Bad query param (e.g. `category=FOO`) | 400 | `MethodArgumentTypeMismatchException` |
| Risk or mitigation not found, or mitigation belongs to another risk | 404 | `NotFoundException` |
| Closed-rule violation | 409 | `BusinessRuleException` |

Error body (`application/problem+json`):
```json
{ "type": "about:blank", "title": "Validation failed", "status": 400,
  "detail": "Request validation failed",
  "errors": [ { "field": "likelihood", "message": "must be an integer between 1 and 5" } ] }
```

---

## 4. Scoring specification (the critical part)

**Inherent** = `likelihood × impact` (1..25).

**Residual** (`DiminishingReturnsResidualCalculator`):

1. Each mitigation with effectiveness `e` (1..5) removes `15% × e` of the risk: `r = 0.15·e` (range 15%..75%).
2. Mitigations combine multiplicatively, so each later control reduces what remains rather than the original. Two controls never add up to more than 100%.
   `remainingFactor = Π (1 − r_i)`
3. Cap total reduction at 90%, so `remainingFactor ≥ 0.10`. No set of controls drives risk to zero.
4. `residual = max(1, round_HALF_UP(inherent × remainingFactor))`
5. Compute with `BigDecimal`. Integer percentages (`100 − 15e`) avoid floating-point rounding surprises at `.5` boundaries.

Sanity checks, all unit-tested:

| Inherent | Mitigations (effectiveness) | Factor | Residual |
|----------|------------------------------|--------|----------|
| 20 | none | 1.00 | **20** (= inherent) |
| 20 | [5] | 0.25 | **5** (meaningfully lower) |
| 20 | [3, 3] | 0.55² = 0.3025 | 6 |
| 25 | [5, 5, 5] | 0.0156, capped at 0.10 | 3 |
| 1 | [5] | 0.25 | 1 (never below 1) |
| 3 | [1] | 0.85 | 3 (rounds back up; low-effect control on a tiny score) |

Properties asserted in tests:
- Residual never exceeds inherent.
- Residual is ≥ 1.
- Adding a mitigation never increases residual (monotonic).
- Order of mitigations doesn't matter.

Severity bands for both scores: LOW 1–5, MEDIUM 6–12, HIGH 13–19, CRITICAL 20–25. Boundary tests cover 1, 5, 6, 12, 13, 19, 20 and 25. 0 and 26 throw.

---

## 5. Database design (PostgreSQL)

Flyway `V1__init.sql`:

```sql
CREATE TABLE risks (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title       VARCHAR(200)  NOT NULL,
    description VARCHAR(2000),
    category    VARCHAR(20)   NOT NULL
                CHECK (category IN ('OPERATIONAL','FINANCIAL','COMPLIANCE','SECURITY','STRATEGIC')),
    owner       VARCHAR(100)  NOT NULL,
    likelihood  SMALLINT      NOT NULL CHECK (likelihood BETWEEN 1 AND 5),
    impact      SMALLINT      NOT NULL CHECK (impact BETWEEN 1 AND 5),
    status      VARCHAR(20)   NOT NULL CHECK (status IN ('OPEN','MITIGATING','CLOSED')),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_risks_category ON risks(category);
CREATE INDEX idx_risks_status   ON risks(status);

CREATE TABLE mitigations (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    risk_id       BIGINT       NOT NULL REFERENCES risks(id) ON DELETE CASCADE,
    description   VARCHAR(1000) NOT NULL,
    effectiveness SMALLINT     NOT NULL CHECK (effectiveness BETWEEN 1 AND 5),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_mitigations_risk_id ON mitigations(risk_id);
```

```
risks 1 ────── 0..* mitigations
```

- Enums are stored as `VARCHAR` (`@Enumerated(STRING)`), never ordinals. CHECK constraints back them up, so bad data can't enter even if the API is bypassed.
- Scores are not columns (D1).
- `createdAt` and `updatedAt` are set by `@PrePersist` and `@PreUpdate`. Mitigation changes do not bump the risk's `updatedAt` (noted in the README).
- Tests run on H2 in PostgreSQL mode, so no Docker is needed. Dev and prod use PostgreSQL. The trade-off is documented in the README: H2 can hide Postgres-specific behaviour, and the migration is kept to portable SQL for that reason.

---

## 6. Business rule: closing a risk

**Rule:** a risk cannot be `CLOSED` unless it has at least one mitigation. The API returns **409 Conflict**:
`"A risk cannot be closed without at least one mitigation. Add a mitigation first."`

Why:
- In a compliance product, "closed" tells an auditor the risk was treated. A closed risk with no controls looks like an unaddressed risk that was hidden.
- Applies to **create** with `status=CLOSED` as well, since a new risk has zero mitigations.
- Symmetric invariant: **deleting the last mitigation of a CLOSED risk** is also 409. Without it the rule could be bypassed by deleting after closing.
- 409 (state conflict) is used rather than 400, since the payload is well-formed and it is the current resource state that disallows it.
- Intentionally skipped: a "risk accepted" or "closed with justification" path. This is noted in the README under "with more time".

---

## 7. Frontend design (React 18, TypeScript, Vite)

### 7.1 Stack and patterns

| Concern | Choice | Why |
|---------|--------|-----|
| Server state | **TanStack Query** | Caching, loading and error states, and invalidation after mutations. This is what makes residual refresh after adding a mitigation. |
| Routing | React Router | 4 simple routes |
| Forms | react-hook-form + zod | Typed validation, live `watch` for the inherent preview |
| Filter and sort state | **URL search params** | Shareable and survives refresh, with no extra store |
| Styling | Plain CSS with variables | Visual polish is out of scope |
| Tests | Vitest + React Testing Library | |

Patterns: **feature-based folders**, **container/presentational split** (pages fetch, components render), **custom hooks per query or mutation**, a **query-key factory**, and a **single typed API client** that normalises errors into an `ApiError`.

### 7.2 Folder structure

```
frontend/src
├── api
│   ├── client.ts            fetch wrapper → throws ApiError {status, title, fieldErrors}
│   ├── risks.ts             typed functions per endpoint
│   └── types.ts             Risk, RiskSummary, Mitigation, enums (mirror backend DTOs)
├── lib
│   └── scoring.ts           inherentScore(l,i), severityBand(score), used by the form preview only
├── components
│   ├── SeverityBadge.tsx    colour + text label for a score and band
│   └── ErrorMessage.tsx, Loading.tsx
├── features/risks
│   ├── hooks.ts             useRisks(filters), useRisk(id), useCreateRisk, useUpdateRisk,
│   │                        useDeleteRisk, useAddMitigation, useDeleteMitigation, riskKeys
│   ├── DashboardPage.tsx    container: reads URL params, calls useRisks
│   ├── RiskFilters.tsx      category, status, sort controls
│   ├── RiskTable.tsx        presentational
│   ├── RiskFormPage.tsx     create and edit (container)
│   ├── RiskForm.tsx         presentational form + live inherent preview
│   ├── RiskDetailPage.tsx   container
│   ├── MitigationList.tsx
│   └── MitigationForm.tsx
├── App.tsx                  routes
└── main.tsx                 QueryClientProvider, Router
```

### 7.3 Routes and screens

| Route | Screen |
|-------|--------|
| `/` | **Dashboard:** filters (category, status), sort toggle for residual (default desc), table with columns Title, Category, Status, Inherent, Residual, Mitigations. Rows link to detail. |
| `/risks/new` | **Create form** |
| `/risks/:id/edit` | **Edit form** (same component, prefilled) |
| `/risks/:id` | **Detail:** risk fields, inherent and residual badges, mitigation list (with delete), add-mitigation form, Edit and Delete buttons |

### 7.4 Severity communication

`SeverityBadge` shows the score plus the band name, coloured by band. Colour is never the only signal.

| Band | Colour | Label |
|------|--------|-------|
| Low | green | "5 · Low" |
| Medium | yellow | "9 · Medium" |
| High | orange | "15 · High" |
| Critical | red | "22 · Critical" |

The dashboard's default order (residual descending) puts the most severe risks at the top.

### 7.5 Data flow

```
DashboardPage ─ URL params {category,status,sort} ─▶ useRisks ─▶ GET /api/risks?...
Add mitigation ─▶ useAddMitigation ─▶ POST /api/risks/{id}/mitigations
                  onSuccess: invalidate riskKeys.detail(id) + riskKeys.lists()
                  ─▶ detail refetches ─▶ residual badge updates
```

Server validation errors (400 with `errors[]`) map onto form fields via `setError`. Business-rule 409s appear as a form-level banner. Loading and error states are minimal: a spinner and an error message with retry. Optimistic updates are out of scope.

### 7.6 Form behaviour

- Fields: title, description, category, owner, likelihood (1–5 select), impact (1–5 select), status.
- Live preview: `watch(likelihood, impact)` → `<SeverityBadge score={likelihood*impact} />` labelled "Inherent score".
- Client-side zod rules match the server (1–5 integers). The server stays authoritative.
- Picking `CLOSED` on a risk with no mitigations is still submitted. The API's 409 message is shown (rule lives in one place, the server).

---

## 8. Testing strategy

**Backend**

| Layer | Tests |
|-------|-------|
| `SeverityBand` (unit, most important) | Parameterised boundaries 1, 5, 6, 12, 13, 19, 20, 25. Out-of-range 0 and 26 throw. |
| `RiskScoring.inherent` (unit) | Full 5×5 grid is within 1..25 and equals l×i. |
| `DiminishingReturnsResidualCalculator` (unit) | Table in §4, plus properties: ≤ inherent, ≥ 1, monotonic, order-independent, 90% cap, rounding at `.5`. |
| `RiskService` / `MitigationService` (unit, Mockito) | Closed rule on create, update, and last-mitigation delete. Closing succeeds with one mitigation. 404 cases. |
| API integration (`@SpringBootTest` + MockMvc, H2) | E2E: create risk (4×5) → add mitigation (5) → GET → `residualScore=5`, `CRITICAL`→`LOW`. Also: invalid likelihood/impact/effectiveness (0, 6, 3.5, "x") → 400 with field message; close with zero mitigations → 409; filter by category/status; sort by residual desc; delete cascades. |

**Frontend (Vitest/RTL):** `scoring.ts` boundary tests, `SeverityBadge` renders the right label and class per band, and `RiskForm` updates the inherent preview when likelihood or impact change.

Commands: `cd backend && mvn test` and `cd frontend && npm test`.

---

## 9. Run and config

- `backend/src/main/resources/application.yml`: Postgres URL, user and password from env vars with local defaults (`localhost:5432/riskregister`). `ddl-auto: validate`, Flyway enabled.
- `application-test.yml`: H2 in PostgreSQL mode.
- Vite dev server proxies `/api` to `http://localhost:8080`, so no CORS config is needed.
- Local Postgres setup in README, with a single `createdb riskregister` or one-line `docker run` for convenience.

---

## 10. Assumptions and intentionally skipped

- Single implicit organization, no auth.
- No pagination (the list is returned whole). Sorting happens in memory.
- No optimistic locking. Last write wins on PUT.
- No mitigation edit UI (the API supports it).
- No audit history of score changes.
- No "accepted risk" closure path.
- The effectiveness scale is defined as 1 = barely reduces the risk, 5 = nearly eliminates it.

---

## 11. Build order

1. `scoring` package and its unit tests (the foundation).
2. Flyway migration, entities, repositories.
3. Services (with the closed rule) and their unit tests.
4. Controllers, DTOs, validation, error handler, and the integration test.
5. Frontend API client, types, hooks.
6. Dashboard, then form, then detail and mitigations.
7. README (formula, trade-offs, what I'd do with more time).
