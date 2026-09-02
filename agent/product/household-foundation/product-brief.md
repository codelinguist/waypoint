# Product Brief: Household Foundation

## Status

`READY`

## Ownership

- Product Owner Agent: Waypoint Product Owner Agent
- User(s): Ralph and his wife
- Created at: 2026-09-03
- Last updated at: 2026-09-03

## User input

- Problem as presented: Waypoint needs its first canonical, persisted household
  structure before it can safely attach financial state, plans, or decisions to
  the family.
- Examples or evidence supplied: The documented initial household consists of
  Ralph, his wife, and three children. The roadmap places `Household`, `Person`,
  a basic API, persistence, migrations, and tests in Phase 1.
- Corrections and constraints supplied: Build for this private household first;
  minimize sensitive personal data; use a Java/Spring Boot modular monolith with
  PostgreSQL; do not add authentication, financial records, AI, or distributed
  infrastructure in this increment.
- Explicit preferences: Ralph prefers systems thinking, clear rules, measurable
  outcomes, and practical tradeoff explanations. The household should not incur
  unnecessary bookkeeping burden.

## Product framing

- Underlying problem: Waypoint has no durable planning-unit identity or member
  boundary. Without it, later financial records cannot be associated with a
  canonical household and conversation risks becoming the de facto datastore.
- Primary user: Ralph and his wife as members of one private household.
- Desired outcome: A caller can create and retrieve a household, add a minimally
  identified person to it, and retrieve its members from durable storage.
- Success measure: All four required behaviors work against PostgreSQL after a
  clean migration, reject invalid input predictably, and are covered by automated
  tests and usable local setup instructions.
- Priority and rationale: Highest current priority. This is the first Phase 1
  vertical increment and is a prerequisite for assets, liabilities, income,
  goals, plans, decisions, and snapshots.

## Knowledge classification

### Confirmed inputs

- The initial product is private and tailored to Ralph and his wife rather than
  a generic multi-tenant SaaS product.
- The documented household includes Ralph, his wife, and three children.
- `Household` requires an ID, name, base currency, and creation/update
  timestamps.
- `Person` requires an ID, household association, name, role, and
  creation/update timestamps.
- The Java application and PostgreSQL database own canonical household state.
- Application code, not an LLM or chat history, must enforce and persist state.
- Authentication, authorization, financial entities, AI behavior, external
  integrations, and UI are outside Task 001.

### Product assumptions to validate

- API callers will enter the household and people; repository research is
  context, not authorization to pre-populate canonical records.
- A person's `role` is descriptive household context in this increment. It does
  not grant application permissions or financial approval authority.
- One household is the user-zero expectation, but the persistence model and API
  need not enforce a global one-household limit.
- Names and roles can be stored as provided after whitespace normalization;
  exact spelling and role wording belong to the users.
- A three-letter uppercase currency code is sufficient validation for the first
  increment. Validation against a maintained currency catalogue can wait until
  currency-dependent calculations require it.

### Open questions

- The durable vocabulary for person roles (for example, adult, child,
  dependent, or planner) is not yet established. Free text is intentionally used
  for this increment so Task 001 does not invent future authorization or
  dependency semantics.
- Update, deletion, archival, and household-membership transfer behavior are not
  yet defined. They are not needed to prove this foundation and remain follow-up
  discovery.
- Authentication and access control must be defined before the API is exposed
  beyond a trusted local/private environment.

## Scope

### In scope

- A runnable Spring Boot backend scaffold.
- PostgreSQL configuration through environment variables.
- JPA/Hibernate persistence and Flyway-managed schema creation.
- Minimal `Household` and `Person` records and their association.
- Validated REST operations to create and retrieve a household and to add and
  retrieve its members.
- Predictable not-found and validation responses.
- Automated domain/API/persistence coverage proportionate to the increment.
- Local setup, migration, run, and test instructions.

### Out of scope

- UI or design exploration.
- Authentication, authorization, invitations, or multi-user collaboration.
- Updates, deletion, archival, membership transfer, or merge behavior.
- Assets, liabilities, income, expenses, transactions, goals, facts,
  assumptions, recommendations, decisions, plans, snapshots, or scenarios.
- Seeding Ralph's family or any documented financial/personal details.
- AI features, external integrations, queues, event sourcing, vector storage,
  Kubernetes, or additional services.

## User flow or behavior

1. A trusted local caller submits a non-blank household name and base currency.
2. Waypoint validates the request, persists the household, and returns its
   server-assigned identity and timestamps.
3. The caller can retrieve that household by ID.
4. The caller submits a non-blank person name and role against the household ID.
5. Waypoint validates that the household exists, persists the member with that
   association, and returns the created member.
6. The caller retrieves the household's member collection; a new household has
   an empty collection, and added members appear in deterministic creation order.
7. Invalid input or an unknown household ID produces a clear client-facing error
   without creating partial or orphaned records.

## Acceptance criteria

- [ ] On a clean PostgreSQL database, the application starts and Flyway creates
  the required household and person schema without manual SQL steps.
- [ ] Creating a household with a non-blank name and a valid three-letter base
  currency returns a generated UUID, normalized uppercase currency, and
  populated creation/update timestamps, and the record remains retrievable.
- [ ] Blank names, blank roles, and malformed currency codes are rejected with a
  validation response and do not persist records.
- [ ] Retrieving an existing household returns its ID, name, base currency, and
  timestamps; retrieving an unknown UUID returns a not-found response.
- [ ] Adding a person to an existing household returns a generated UUID, the
  household association, name, role, and timestamps, and persists the member.
- [ ] Adding a person to an unknown household returns a not-found response and
  does not create an orphaned person.
- [ ] Retrieving members for a new household returns an empty collection;
  retrieving after additions returns every member for that household only in
  deterministic creation order.
- [ ] Duplicate person names within a household are permitted because a name is
  not a reliable unique identity and no disambiguation rule is documented yet.
- [ ] Person records contain no date of birth, contact details, government IDs,
  financial values, or other sensitive fields outside the approved minimum.
- [ ] Automated tests cover successful creation/retrieval, validation failures,
  unknown-household behavior, household isolation, and the PostgreSQL/Flyway
  boundary where relevant.
- [ ] The README documents prerequisites, environment variables, database setup,
  application startup, migrations, and test commands that a developer can
  follow locally.
- [ ] No out-of-scope infrastructure or domain feature is introduced.

## Risks and safeguards

- Financial-data or household-approval boundary: This increment stores identity
  structure only. Repository research must not be silently converted into seeded
  household records, and creating a person does not authorize financial actions
  for that person.
- Privacy or sensitive-data considerations: Collect only name and descriptive
  role, avoid logging request bodies as application behavior, keep secrets out of
  source control, and retain private/local deployment as the expected posture.
- Accessibility considerations: No UI is in scope. Error payloads and API
  documentation should use clear, non-ambiguous language for future accessible
  clients.
- Failure or misuse risks: Orphan members, cross-household member leakage,
  ambiguous validation, and accidental public exposure are the principal risks.
  Use a database foreign key, household-scoped retrieval, explicit error
  responses, and trusted local/private operation without implying production
  security.

## Product decisions

### PD-001 — Task 001 is ready for implementation

- Decision: Authorize Task 001 as a backend-only implementation increment.
- Evidence: Phase 1 explicitly requires Household, Person, basic API,
  persistence, migrations, and tests; the current task supplies the stack,
  boundaries, and Definition of Done; D009, D011, and D012 settle the runtime and
  persistence direction.
- Alternatives considered: Continue discovery; add the frontend; combine this
  with assets and liabilities.
- Rationale: The user outcome and boundaries are testable now. A UI or broader
  financial model would add scope without proving the canonical household
  foundation.
- User input required: `NO`

### PD-002 — Do not seed household members from research documents

- Decision: Require explicit API input to create canonical household and person
  records; do not turn documented user-zero context into seed data.
- Evidence: Structured application data is canonical, inferred values must not
  become facts silently, and material changes require user approval.
- Alternatives considered: Seed Ralph, his wife, and three unnamed child
  records automatically.
- Rationale: Research establishes product context but is not a canonical-data
  write request. Explicit creation preserves provenance and avoids inventing
  names or roles.
- User input required: `NO`

### PD-003 — Keep person identity minimal and role non-authoritative

- Decision: Store only the required name, descriptive role, household
  association, ID, and timestamps. Treat role as bounded free text rather than
  an authorization or household-decision enum.
- Evidence: The financial model marks additional person fields as potential and
  directs minimization; Task 001 explicitly excludes unnecessary sensitive
  information and authentication.
- Alternatives considered: Add birth dates and dependent status now; define a
  fixed role taxonomy; use role for permissions.
- Rationale: The minimal record supports the next roadmap increments without
  prematurely fixing policy or collecting sensitive data.
- User input required: `NO`

### PD-004 — Use simple, predictable creation and retrieval semantics

- Decision: Reject blank required fields, normalize base currency to uppercase,
  use not-found behavior for unknown household IDs, return an empty member
  collection for a household with no members, and sort members by creation time
  with ID as a stable tie-breaker.
- Evidence: The product favors explicit state, clear rules, measurable outcomes,
  and simple auditable designs. These semantics make the Definition of Done
  objectively testable.
- Alternatives considered: Return null for no members; silently accept malformed
  values; leave list order database-dependent.
- Rationale: These choices are conventional, reversible, and prevent ambiguous
  client behavior without committing future product policy.
- User input required: `NO`

### PD-005 — Permit duplicate names

- Decision: Do not use household name or person name as a uniqueness key.
- Evidence: Names are descriptive fields, server UUIDs are required identities,
  and the repository defines no uniqueness rule.
- Alternatives considered: Enforce unique household names globally or unique
  person names within a household.
- Rationale: Real households can contain identical names, and a private product
  may later retain historical or similarly named households. UUIDs provide the
  unambiguous identity required now.
- User input required: `NO`

## Delivery handoff

- Current task: `agent/current-task.md` — Task 001, Scaffold the Backend and
  Household Aggregate
- Design brief, if applicable: Not applicable; no UI is in scope.
- Implementation owner: Codex
- Review evidence: Pending implementation, automated test results, clean-database
  migration evidence, and README setup verification.

## Feature acceptance

- Acceptance status: `PENDING`
- Acceptance evidence: Not yet implemented.
- Unmet criteria: All acceptance criteria remain pending.
- Returned work: None.
- Follow-up opportunities: Define member lifecycle and role taxonomy only when a
  concrete downstream feature requires them; define authentication before any
  non-private deployment.
- Accepted or returned by Product Owner Agent: Pending
- Accepted or returned at: Pending
