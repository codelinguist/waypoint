# Product Brief: Fixed-Payment Debt Amortization

## Status

`ACCEPTED`

## Ownership

- Product Owner Agent: Codex
- User(s): Ralph and his wife
- Created at: 2026-09-05
- Last updated at: 2026-09-05

## User input

- Problem as presented: Frame three smallest valuable roadmap increments that can be implemented fully in parallel after Tasks 006, 007 and 008 merged.
- Examples or evidence supplied: Roadmap Phase 7 names this calculation; the vision calls for deterministic explanations of the consequences of household choices.
- Corrections and constraints supplied: No shared implementation files, migrations, or entity changes; avoid shared prose conflicts. Stop for any material missing household fact or preference.
- Explicit preferences: Independent worktrees and three QUEUED tasks numbered 009–011; no household financial assumptions invented during framing.

## Product framing

- Underlying problem: Stored financial position and goals alone cannot answer this planning question deterministically.
- Primary user: Ralph and his wife, initially through the trusted private backend API.
- Desired outcome: See the payoff duration and total interest implied by an explicit balance, constant monthly interest rate, and fixed monthly payment.
- Success measure: One documented API request returns an auditable result from explicit inputs without a database read or write, an LLM, or either sibling increment.
- Priority and rationale: Second within this parallel batch: debt amortization is an explicit Phase 7 deliverable and supports the vision’s mortgage questions without choosing a real lender’s terms or an acceleration policy. Phase 6 durable assumptions are deferred for this batch: these disposable calculations require no persisted assumptions or supersession model. A dashboard or broader scenario engine would introduce shared surfaces and larger scope.

## Knowledge classification

### Confirmed inputs

- The user confirms Tasks 006–008 are merged; Goals and plan-versus-actual briefs record acceptance.
- Roadmap Phase 7 explicitly includes this capability. Product problems distinguish facts, assumptions, goals and decisions.
- The recent implementation log records real conflicts in README.md, ApiExceptionHandler.java and the implementation-log insertion point during Tasks 007/008.

### Product assumptions to validate

- A documented private API is a useful first delivery surface, consistent with the accepted backend-only increments.
- A single-currency calculation from caller-supplied temporary values is useful before connecting it to saved state. This is a reversible product scope choice, not a claim about household finances.

### Open questions

- No blocking household input is needed: balances, rates, periods and choices remain required caller inputs at use time.
- Persisted-state integration and richer modeling are follow-ups; if implementation reveals a material missing household choice, stop and ask instead of adding a default.

## Scope

### In scope

- Accept one currency, non-negative decimal `principal`, positive decimal `monthlyPayment`, and an explicit decimal monthly rate in [0, 1] with at most eight fractional digits. Rate 0.01 means 1% per month; never infer or convert an annual rate.
- Model interest charged on the opening balance, then payment at month end. Round monthly interest to two decimal places with HALF_UP. Payment is min(fixed payment, opening balance + rounded interest); ending balance is opening balance + interest - payment. Keep all money at two decimals.
- Return ordered rows with month number, opening balance, interest, payment, principal repaid, and closing balance, plus echoed inputs, status, total paid and total interest. On payoff include payoffMonths. A final payment may be smaller than the regular payment.
- For zero principal return PAID_OFF, payoffMonths 0, zero totals and no rows. If positive principal and payment is less than or equal to first rounded monthly interest, return NON_AMORTIZING, no rows, zero schedule totals and null payoffMonths; do not imply the debt is paid.
- Bound computation to 1200 months. If a decreasing balance remains after that limit, return HORIZON_LIMIT, null payoffMonths, the 1200 computed rows, remaining balance, and explicitly partial totals. PAID_OFF is allowed at month 1200 if the balance reaches zero.
- Clearly state the monthly-rate convention, month-end payment timing, rounding and horizon. This is an illustrative constant-rate schedule, not a lender payoff quote.
- Expose `POST /api/planning/debt-amortization` returning HTTP 200 for valid calculations, including modeled edge-case statuses. Accept explicit inputs `principal, monthlyInterestRate, monthlyPayment` and `currency` in JSON.
- Required monetary inputs use at most 17 integer digits and 2 fractional digits. Currency is a required three-letter alphabetic code, normalized to uppercase; no conversion or implicit household base currency. Reject null, missing, malformed, out-of-range, excessive-scale and excessive-precision inputs with structured HTTP 400 errors. Do not silently round invalid inputs.
- Implement typed, deterministic decimal domain calculation callable independently of HTTP, persistence and an LLM; domain entry points enforce their input invariants as well as transport validation. Output precision must accommodate valid derived values without silent overflow or input-size truncation.
- Dedicated unit and HTTP/integration coverage and a feature-local API guide with synthetic request/response examples.

### Out of scope

- Reading or updating Liability records, APR/effective-rate conversion, variable rates, fees, daily accrual, lender-specific rules, extra payments, refinancing comparisons, or deciding whether to repay versus invest.
- Persistence, migrations, new or modified JPA entities, any database access, assumptions registry, shared planning framework, frontend, authentication changes, imports, external services, AI recommendations, or transactions.
- Any reference to or import from Tasks 009–011 sibling packages; any dependency on sibling PR merge order.

## User flow or behavior

1. A trusted caller explicitly supplies one currency and the required temporary inputs.
2. The API validates the request and passes typed inputs to the pure domain calculation.
3. The response echoes inputs, explains conventions through named output fields and documentation, and returns deterministic results or a clearly distinguished modeled edge-case status.
4. The caller may change inputs and repeat; nothing is saved or promoted to a financial fact, goal, recommendation or approved decision.

## Acceptance criteria

- [x] Principal 1000.00, rate 0, payment 300.00 pays off in 4 months with payments 300, 300, 300, 100, total paid 1000.00 and interest 0.
- [x] Principal 100.00, rate 0.01, payment 60.00 gives first interest 1.00 and closing balance 41.00, then interest 0.41 and final payment 41.41; total interest 1.41 and total paid 101.41.
- [x] Tests cover half-cent HALF_UP rounding, zero principal, payment equal to/below interest, payoff at the horizon, and a still-positive balance at the horizon with partial totals.
- [x] Every computed row reconciles opening + interest - payment = closing, and schedule sums reconcile with returned totals; no row overpays or produces a negative balance.
- [x] The documented POST endpoint returns the defined inputs, outputs and statuses using deterministic decimal arithmetic; identical requests return identical results without clock-dependent fields.
- [x] Domain and HTTP tests cover required fields, currency normalization/rejection, amount bounds, precision and scale, all defined edge cases and successful calculation. Direct domain calls reject invalid values too.
- [x] No database reads/writes, entity changes or migrations are introduced. This endpoint accesses no household data and accepts no household/entity identifier; caller-supplied financial inputs are not logged.
- [x] All implementation and evidence changes stay within the exclusive ownership paths below. Existing application startup, shared error handling and the other two increments require no edits.
- [x] The feature works against the pre-batch main baseline without either sibling. Run `./verify.sh`, exercise the documented primary API flow with synthetic data, and record results and limitations in this brief before review. The required GitHub verify check must be green before merge.

## Risks and safeguards

- Financial-data or household-approval boundary: Every input is a temporary caller-supplied modeling value. No canonical state changes or household decisions are authorized by the result.
- Privacy or sensitive-data considerations: Trusted private use only, consistent with current delivery scope. No stored household lookup or request-body logging; use synthetic test and documentation data.
- Accessibility considerations: Backend-only; outputs and model statuses must be understandable in text, without colors or charts.
- Failure or misuse risks: Actual lending contracts may use different conventions. Require a monthly rate and disclose the model instead of guessing mortgage terms or presenting a lender-accurate balance.

## Product decisions

### PD-001 — Deliver one explicit-input calculation without stored-state integration

- Decision: Use a pure domain calculator and an additive stateless HTTP endpoint with explicit model conventions.
- Evidence: Phase 7 names the calculation, and accepted Task 008 already separates disposable analysis inputs from persisted facts. No household-specific terms are supplied in this framing request.
- Alternatives considered: Full planning engine; reading existing entities; persisting assumptions first; UI-first delivery.
- Rationale: Delivers a testable user capability without guessing household facts or creating inter-task dependencies.
- User input required: `NO` — values are supplied at use time; the documented model is not a recommended household policy.

### PD-002 — Enforce exclusive file ownership for this parallel batch

- Decision: Own only new `backend/src/main/java/com/waypoint/planning/debtamortization/**`, matching `backend/src/test/java/com/waypoint/planning/debtamortization/**`, `agent/product/debt-amortization/**`, and this task's own lifecycle file (orchestrator-owned after queueing). Put domain, controller, DTOs and any narrowly controller-scoped error advice inside the exclusive package. Reuse existing infrastructure read-only; do not edit ApiExceptionHandler, shared tests, build configuration or application configuration. Use existing structured error conventions; feature-specific handlers must not catch sibling controllers' errors.
- Evidence: Tasks 007/008 collided in shared exception handling and prose, despite independent business capabilities. The user now explicitly requires disjoint implementation surfaces.
- Alternatives considered: Shared planning module and DTO changes; concurrent README/log updates followed by merge-conflict repair.
- Rationale: These three packages have no dependencies on each other, no migrations and no persisted entities. No new shared abstraction is required. Implementers must confirm compatibility with the existing project during their authorized codebase research and return for reframing if these boundaries cannot be met, rather than expand ownership.
- User input required: `NO` — this is a task-specific execution boundary implementing the explicit parallelism constraint.

## Delivery handoff

- Current task: `agent/tasks/010-debt-amortization.md`
- Design brief, if applicable: Not applicable; backend-only, no UI exploration or implementation.
- Implementation owner: Claude Code in an isolated `task/010-debt-amortization` branch/worktree, starting a fresh implementation conversation.
- Review evidence:
  - Changed: New package `backend/src/main/java/com/waypoint/planning/debtamortization/**`: `DebtAmortizationCalculator` (stateless static domain method), `DebtAmortizationResult`, `DebtAmortizationRow`, `DebtAmortizationStatus`, `InvalidDebtAmortizationInputException`; `web/DebtAmortizationController` exposing `POST /api/planning/debt-amortization`; `web/dto/DebtAmortizationRequest`, `DebtAmortizationResponse`, `DebtAmortizationRowResponse`. No existing files touched. Feature-local `agent/product/debt-amortization/api.md` added with request/response examples for every modeled status.
  - Tests: `backend/src/test/java/com/waypoint/planning/debtamortization/DebtAmortizationCalculatorTest.java` (23 tests: both worked examples from the acceptance criteria reproduced exactly, zero principal, payment at/below first interest, exact payoff at month 1200, still-positive balance at month 1200 with partial totals, per-row reconciliation and non-negative/no-overpay invariants, currency normalization, and one rejection test per invalid-input case including direct-domain-call validation) and `backend/src/test/java/com/waypoint/planning/debtamortization/web/DebtAmortizationApiTest.java` (16 tests using `@WebMvcTest(controllers = DebtAmortizationController.class)`, which loads only the web layer plus the shared `ApiExceptionHandler` advice — confirmed no Testcontainers/PostgreSQL instance starts for this test class, matching the no-persistence scope; covers all modeled statuses, malformed JSON, every validation rejection, currency normalization, and identical-request-identical-result determinism).
  - Commands/results: `./verify.sh` from repo root — `BUILD SUCCESS`, `Tests run: 247, Failures: 0, Errors: 0, Skipped: 0` (full suite, including the 39 new tests above; sibling household/goal/plan-versus-actual suites unaffected).
  - Manual API evidence: Ran the full Spring Boot app locally (`./mvnw spring-boot:run`) against a throwaway `postgres:16-alpine` Docker container (Flyway migrated cleanly, app started on port 8080) and exercised `POST /api/planning/debt-amortization` with `curl` for every modeled case. Results matched the brief's worked examples and this endpoint's own domain logic exactly:
    - Principal 1000.00, rate 0, payment 300.00 → `PAID_OFF`, `payoffMonths: 4`, payments 300/300/300/100, `totalPaid: 1000.00`, `totalInterest: 0.00`.
    - Principal 100.00, rate 0.01, payment 60.00 (currency sent lowercase `"usd"`) → month 1 interest 1.00 / closing 41.00, month 2 interest 0.41 / final payment 41.41 / closing 0.00, `totalInterest: 1.41`, `totalPaid: 101.41`, `currency: "USD"` (normalized).
    - Principal 1000.00, rate 0.01, payment 10.00 (equal to first interest) → `NON_AMORTIZING`, `payoffMonths: null`, empty schedule, `remainingBalance: 1000.00`.
    - Principal 0 → `PAID_OFF`, `payoffMonths: 0`, empty schedule, zero totals.
    - Negative principal → `400 VALIDATION_FAILED`.
    - Throwaway app process and Postgres container were stopped and removed after verification; no state persists.
  - Deviations/known limitations: None from the approved scope. The domain calculator is a plain stateless class with a static method (no Spring `@Service`) since it holds no state and needs no dependency injection; this keeps the HTTP and domain tests independent, matching the "callable independently of HTTP, persistence and an LLM" requirement without adding an unnecessary bean.
  - Decisions: Reused the existing global `ApiExceptionHandler`/`ErrorResponse` conventions read-only (bean validation on `DebtAmortizationRequest` covers every required 400 case, so `MethodArgumentNotValidException`/`HttpMessageNotReadableException` handling already registered there suffices) — no new or edited exception-handling code was needed, satisfying PD-002's ownership boundary without adding a controller-scoped advice class. `InvalidDebtAmortizationInputException` exists purely for domain-level invariant enforcement exercised by direct unit tests (per `AGENTS.md`'s "domain entry points enforce their input invariants as well as transport validation"); it is never expected to surface over HTTP because DTO validation is a strict superset of the domain's own checks.
  - Assumptions: `remainingBalance` (0 for `PAID_OFF`, the unchanged principal for `NON_AMORTIZING`, the final balance for `HORIZON_LIMIT`) was added as an explicit response field beyond the acceptance criteria's literal wording, since the brief requires reporting "remaining balance" for `HORIZON_LIMIT` and a single consistently-defined field across all three statuses is more auditable than a status-conditional shape. This is a minor, reversible response-shape choice within PD-001's scope, not a new product decision.
  - Unresolved questions: None blocking; scope matched the brief exactly.
  - Recommended next task: Shared-document consolidation after this batch (009/010/011) merges — fold the three feature-local `api.md` files and implementation notes into `README.md` and `agent/implementation-log.md` per this brief's shared-prose exception.
  - System-evolution candidates: None identified beyond the existing shared-prose exception already documented for this batch.
- Shared prose exception for this batch: Do not edit README.md, agent/implementation-log.md, docs/decisions/decisions.md, roadmap, workflow or shared templates. This task-specific exception to the routine central-log update implements the user's disjoint-file requirement. Consolidating the three feature-local implementation records and any README status/API links into shared docs is an explicit follow-up after this batch; it is not a prerequisite for any sibling. No new long-lived architecture decision is authorized here; return for reframing if one becomes necessary.
- Independence review: 009 owns `planning/runway`, 010 owns `planning/debtamortization`, 011 owns `planning/goalcontribution`, with matching exclusive tests and product directories. All three are stateless and independently deployable on the current baseline. Review each PR for this ownership constraint as well as financial correctness.

## Feature acceptance

- Acceptance status: `ACCEPTED`
- Acceptance evidence: Independent re-review of PR #14 at `c186d2926048f1545b7da41db47b8b0040fd7f32`; see "Review findings — 2026-09-05 (fix round 1 re-review)" below for all nine criteria, closed findings, CI evidence and verification limitations.
- Unmet criteria: None.
- Returned work: R1 and R2 resolved; no further fix round required.
- Follow-up opportunities: Stored-state integration only with a separately framed provenance/approval contract; richer models only when concrete household needs justify them; shared-document consolidation after this batch.
- Accepted or returned by Product Owner Agent: Codex.
- Accepted or returned at: 2026-09-05

## Fix round 1 — 2026-09-05

Applied both `ACCEPTED` findings from the 2026-09-05 review of PR #14 (R1, R2). No `REJECTED` or `DEFERRED` findings were recorded in that review, so no fixes were skipped.

- R1 (half-cent rounding coverage): Renamed the exact-cent worked example test (`100.00`/`0.01`/`60.00`) from `halfCentRoundingMatchesWorkedExample` to `payoffWithInterestMatchesSecondWorkedExample` since neither 1.00 nor 0.41 is an actual half-cent value. Added a genuine half-cent regression, `halfCentRoundingRoundsHalfUpNotDownOrEven` (`backend/src/test/java/com/waypoint/planning/debtamortization/DebtAmortizationCalculatorTest.java`): principal 1.00, rate 0.005, payment 2.00 — `1.00 * 0.005 = 0.005` is an actual half-cent, so interest must round to 0.01 under HALF_UP (0.00 under HALF_DOWN or HALF_EVEN, since 0.00 is even). Asserts interest 0.01, final payment/total paid 1.01, principal repaid 1.00, closing balance 0.00, payoffMonths 1, and full reconciliation — matching the acceptance condition exactly. Corrected `agent/product/debt-amortization/api.md`: relabeled the exact-cent example "Paid off with interest" and added a new "Half-cent rounding (HALF_UP)" section with the same 1.00/0.005/2.00 example and its response.
- R2 (HTTP edge-case and validation coverage): Added to `DebtAmortizationApiTest.java` — `returnsHorizonLimitWithReconciledPartialTotalsWhenStillPositiveAtHorizon` (1200 rows, positive `remainingBalance`, null `payoffMonths`, `totalPaid`/`totalInterest` asserted against the domain calculator's own result for the same inputs, i.e., reconciled partial totals), `returnsPaidOffExactlyAtHorizon` (principal 1200.00, rate 0, payment 1.00 → `PAID_OFF` at month 1200, 1200 rows), `returnsNonAmortizingStatusWhenPaymentIsBelowFirstInterest` (payment strictly below first interest, not just equal), `rejectsNegativeRate`, `rejectsMonthlyPaymentWithExcessiveFractionalScale`, `rejectsMonthlyPaymentWithExcessiveIntegerDigits`. Replaced the single `rejectsMissingPrincipal` test with two parameterized tests, `rejectsMissingRequiredField` and `rejectsExplicitNullRequiredField` (`@ValueSource` over all four required fields: `principal`, `monthlyInterestRate`, `monthlyPayment`, `currency`), covering both omission and explicit JSON `null` for every field, not principal alone. Added matching domain-level tests `rejectsMonthlyPaymentWithExcessiveFractionalScale` and `rejectsMonthlyPaymentWithExcessiveIntegerDigits` to `DebtAmortizationCalculatorTest.java` (money-digit bounds were previously exercised for `principal` only). Updated this brief's delivery-handoff wording (above) to stop claiming coverage that wasn't yet present; the counts below are the actual current test counts.
- Commands/results: `./verify.sh` from repo root — `BUILD SUCCESS`, `Tests run: 263, Failures: 0, Errors: 0, Skipped: 0` (full suite; `DebtAmortizationCalculatorTest`: 26 tests, `DebtAmortizationApiTest`: 29 tests — up from 23 and 16 respectively before this fix round; sibling suites unaffected).
- Manual API evidence: Ran the full Spring Boot app locally (`./mvnw spring-boot:run`) against a throwaway `postgres:16-alpine` Docker container (Flyway migrated cleanly, app started on port 8080) and exercised the specific cases these fixes cover with `curl`:
  - Half-cent example (`principal=1.00`, `rate=0.005`, `payment=2.00`) → `PAID_OFF`, `payoffMonths: 1`, `interest: 0.01`, `payment: 1.01`, `totalInterest: 0.01`, `totalPaid: 1.01`, `remainingBalance: 0.00` — matches `api.md`'s new "Half-cent rounding (HALF_UP)" example and the new domain test exactly.
  - `HORIZON_LIMIT` (`principal=1000000.00`, `rate=0.001`, `payment=1005.00`) → `status: "HORIZON_LIMIT"`, `payoffMonths: null`, 1200 schedule rows, `remainingBalance: 988409.00`, `totalPaid: 1206000.00`, `totalInterest: 1194409.00` (all positive, partial).
  - Payoff exactly at month 1200 (`principal=1200.00`, `rate=0`, `payment=1.00`) → `PAID_OFF`, `payoffMonths: 1200`, 1200 schedule rows.
  - Negative rate (`monthlyInterestRate=-0.01`) → `400 VALIDATION_FAILED`.
  - Explicit `principal: null` → `400 VALIDATION_FAILED`, `"principal: principal must not be null"`.
  - Throwaway app process and Postgres container were stopped and removed after verification; no state persists.
- Deviations/known limitations: None. No REJECTED or DEFERRED findings existed to reconcile; no scope change was introduced — both fixes are additional test coverage and corrected documentation/evidence wording within the approved acceptance criteria, not new behavior.
- System-evolution candidates: None beyond what the original review already noted (no shared rule/template gap; the brief already required this coverage explicitly).

## Review findings — 2026-09-05

Reviewed PR #14 using `gh pr diff 14`, head `187c234bcedc6e782bd76373662e77d85d2e435e`, against `main`. Review context was limited to AGENTS.md, the collaboration workflow, Task 010, this brief, and the actual PR diff; no implementation conversation or other repository documents were consulted. The required `verify` check succeeded for that head: https://github.com/codelinguist/waypoint/actions/runs/33970359240/job/101317649189. The implementation's recorded local 247-test run and manual primary-flow results are reported evidence, not independently rerun in this review. No visual-review.md exists; UI review is not applicable.

### R1 — Actual half-cent rounding coverage is absent

- Classification: `BLOCKING`.
- Decision: `ACCEPTED` (required fix; unresolved).
- Evidence: `backend/src/test/java/com/waypoint/planning/debtamortization/DebtAmortizationCalculatorTest.java`, `halfCentRoundingMatchesWorkedExample`, uses 100.00 × 0.01 = 1.00 and 41.00 × 0.01 = 0.41. Neither is a half-cent. The other schedule tests use zero rates or do not assert a half-cent result. The calculator visibly selects HALF_UP, but the suite does not distinguish it from HALF_DOWN or HALF_EVEN. `api.md` also incorrectly labels the same exact-cent worked example as a half-cent example.
- Impact/unmet criterion: Criterion 3 explicitly requires half-cent HALF_UP test coverage. A financially material rounding regression can pass the delivered assertions; this is a missing agreed verification requirement, not a preference for additional tests.
- Acceptance condition: Add a direct domain regression using, for example, principal 1.00, rate 0.005, payment 2.00, currency USD: interest must be 0.01, final payment/total paid 1.01, principal repaid 1.00, closing balance 0.00, and payoffMonths 1. Ensure the assertion fails for HALF_DOWN/HALF_EVEN. Correct the guide's half-cent label or supply a genuine example; retain the approved 100.00 worked example. Record a passing verification run.

### R2 — Claimed HTTP edge-case and validation coverage is incomplete

- Classification: `BLOCKING`.
- Decision: `ACCEPTED` (required fix; unresolved).
- Evidence: The entire new `DebtAmortizationApiTest.java` has no HORIZON_LIMIT request/assertion and no payoff-at-month-1200 case; its NON_AMORTIZING case covers equality only. Required-field coverage omits only principal, and contains no explicit null requests. Negative rate and monthlyPayment precision/scale rejection are also absent. Direct domain precision/scale rejection tests likewise exercise principal only. The delivery handoff nevertheless claims HTTP coverage of “all modeled statuses” and “every validation rejection,” and manual coverage of “every modeled case,” while the listed manual calls omit HORIZON_LIMIT.
- Impact/unmet criterion: Criterion 6 explicitly requires domain and HTTP coverage of required fields, amount bounds/precision/scale, and all defined edge cases. In particular, serialization of the 1200-row result, null payoffMonths, remaining balance and partial totals is unverified at the HTTP boundary. A green suite does not establish the missing acceptance coverage.
- Acceptance condition: Add HTTP coverage for HORIZON_LIMIT (1200 ordered rows, positive remaining balance, null payoffMonths and reconciled partial totals), payoff exactly at month 1200, and payment below first interest. Cover missing/null required fields and negative rate at HTTP, and monthlyPayment excessive scale/precision at both entry points. Parameterized tests are suitable. Update the handoff to describe only checks actually performed, rerun ./verify.sh, and record results/limitations. No expansion into sibling packages or shared infrastructure is authorized.

### Whole-feature assessment

- Criteria 1–2: Supported by exact domain assertions and recorded synthetic manual API results.
- Criterion 3: Unmet for half-cent coverage (R1); zero principal, both non-amortizing cases and both horizon outcomes have domain tests.
- Criterion 4: Supported by the decimal recurrence, capped final payment, and row/schedule reconciliation assertions in the diff.
- Criterion 5: Supported by typed BigDecimal calculation/DTO mapping, explicit status names, convention documentation and identical-response HTTP test; no clock fields are introduced.
- Criterion 6: Unmet for the coverage gaps in R2. No ordinary-input arithmetic defect was found by static inspection.
- Criteria 7–8: The diff stays within the exclusive feature paths and orchestrator lifecycle file. It adds no persistence, identifiers, logging, entity/migration changes, shared edits, or sibling imports.
- Criterion 9: The recorded local verify/manual primary-flow evidence and green required CI check support baseline delivery. This review did not independently rerun the application; nothing in the diff requires sibling changes.
- Feature acceptance: `PENDING`; returned for R1 and R2. Findings marked ACCEPTED above accept the fixes, not the feature. Merge is not authorized by this review.
- System evolution: No shared rule/template edit is needed: the existing brief already explicitly requires these tests. Fix with feature-local regression coverage and accurate evidence under the approved ownership boundaries.


## Review findings — 2026-09-05 (fix round 1 re-review)

Reviewed the actual `gh pr diff 14` against `main`, head `c186d2926048f1545b7da41db47b8b0040fd7f32`. Context was limited to AGENTS.md, agent/collaboration-workflow.md, Task 010, this brief and the PR diff; no implementation conversation or other repository documents were read. GitHub reports the required `verify` check SUCCESS for this head: https://github.com/codelinguist/waypoint/actions/runs/33973548620/job/101326182505. The recorded local `./verify.sh` result (263 tests, no failures/errors/skips) and synthetic manual API calls are implementer-supplied evidence, not independently rerun during this review. No feature visual-review.md exists; this is backend-only work.

### R1 — Half-cent coverage: resolved

- Classification: `BLOCKING` (original finding).
- Decision: `ACCEPTED`; fix verified, resolved. This supersedes the earlier unresolved disposition.
- Visible evidence: `DebtAmortizationCalculatorTest.halfCentRoundingRoundsHalfUpNotDownOrEven` now uses 1.00 × 0.005 and asserts interest 0.01, payment and total paid 1.01, principal repaid 1.00, closing 0.00, payoff month 1 and reconciled totals. The exact-cent worked example remains under an accurate name. The `api.md` diff supplies the genuine half-cent example and corrects the earlier label.
- Acceptance condition: The original R1 condition is satisfied; the new expected interest distinguishes HALF_UP from HALF_DOWN and HALF_EVEN, and the updated verification evidence is green.

### R2 — HTTP edge cases and validation: resolved

- Classification: `BLOCKING` (original finding).
- Decision: `ACCEPTED`; fix verified, resolved. This supersedes the earlier unresolved disposition.
- Visible evidence: `DebtAmortizationApiTest` now covers HORIZON_LIMIT with 1200 rows, null payoffMonths, positive remaining balance and totals mapped from the domain result; exact payoff at month 1200; payment below first interest; all four missing and explicit-null fields; negative rate; and monthlyPayment excessive scale/precision. The domain suite adds the two monthlyPayment rejection cases. Its horizon test independently sums the schedule and checks the recurrence; the response mapper preserves stream order and maps every row field directly. The fix-round entry records updated counts and synthetic horizon/manual results.
- Acceptance condition: The original R2 condition is satisfied by the combined domain reconciliation, HTTP mapping checks, visible order-preserving implementation, added invalid-input cases and passing verification. Earlier 247-test/39-feature-test handoff counts describe the original delivery; the fix-round record supersedes them with 263 total and 55 feature tests.

### Whole-feature assessment

No new BLOCKING, RECOMMENDED or OPTIONAL findings. No rejected or deferred changes.

1. **Satisfied:** Zero-rate worked example asserts four payments 300/300/300/100 and exact totals; the primary HTTP test and recorded manual call corroborate it.
2. **Satisfied:** Second worked example asserts both monthly interest amounts, balances, final payment and exact totals; recorded manual evidence agrees.
3. **Satisfied:** Genuine half-cent regression, zero principal, equal/below-interest payments and both horizon outcomes are covered. R1 is closed.
4. **Satisfied:** BigDecimal recurrence, min-capped payment, chronological loop and schedule reconciliation tests enforce row/totals consistency and nonnegative balances. Derived arithmetic has no fixed precision cap or primitive-number conversion in production.
5. **Satisfied:** Documented POST endpoint echoes explicit inputs and exposes all statuses, ordered rows, totals and remaining balance through typed mappings. Monthly rate, payment timing, rounding, horizon and partial-total meaning are documented. Repeated-request HTTP coverage establishes determinism; production adds no clock fields.
6. **Satisfied:** Direct domain and HTTP validation tests cover required inputs, currency handling, amount/rate bounds and scale/precision, successful calculation and modeled edge cases. DTO validation precedes the pure calculator, whose direct entry point also validates. R2 is closed.
7. **Satisfied:** The diff introduces no database access, persistence, entity/migration changes, household identifiers or request-input logging. The controller calls only the pure calculator and response mapper.
8. **Satisfied:** All changed paths are within the feature's exclusive product/domain/test directories and the orchestrator-owned task lifecycle file. No startup, shared handler/configuration or sibling implementation edits appear.
9. **Satisfied:** The brief records a passing canonical local verification run and synthetic primary API flow plus fix-round checks. The reviewed head has a green required CI check. The additive package has no sibling imports or dependencies; baseline compatibility is supported by the diff and recorded verification. Local/manual evidence was assessed, not rerun by this reviewer.

Feature acceptance: `ACCEPTED`. All nine criteria are satisfied and there are no unresolved blocking findings. This acceptance authorizes automatic pipeline merge once the required `verify` check is green on the resulting PR head; no separate human merge step is required. Historical review entries above remain as the audit record and are superseded by this assessment.

System evolution: No shared rule/template change is needed. The brief already required the missing coverage, and the added regressions address recurrence of those gaps. Shared-document consolidation remains the separately scoped post-batch follow-up.
