# Product Brief: Automated Delivery Gates

## Status

`ACCEPTED`

## Ownership

- Product Owner Agent: Waypoint Product Owner Agent
- User(s): Ralph and his wife
- Created at: 2026-09-03
- Last updated at: 2026-09-03

## User input

- Problem as presented: The first branch-and-PR delivery cycle worked, but its
  verification and governance still depended on agents manually reproducing
  commands and remembering the merge policy.
- Examples or evidence supplied: PR #1 initially documented a Maven command
  that could not find the project POM, GitHub reported no configured status
  checks, Product Owner acceptance had to be pushed manually, and formal
  GitHub approval was unavailable because the PR author and reviewer used the
  same GitHub identity.
- Corrections and constraints supplied: Preserve the established product brief
  -> Claude implementation -> Codex review -> fix round -> acceptance -> merge
  workflow. Codex may push its completed review artifacts and merge only after
  committing acceptance.
- Explicit preferences: Implement the previously identified "do now"
  improvements without adding speculative multi-agent orchestration.

## Product framing

- Underlying problem: A review can be thoughtful and still produce a false
  sense of safety when the required checks are not reproducible or enforced at
  the repository boundary.
- Primary user: Ralph, operating Waypoint with Claude Code as implementer and
  Codex as Product Owner Agent.
- Desired outcome: Every pull request runs one canonical verification command
  in CI, `main` requires that check before merge, and the documented agent
  workflow unambiguously governs review artifacts, acceptance, and merging.
- Success measure: A representative PR automatically reports the canonical
  verification result; GitHub blocks merging when that required check fails or
  is absent; and both agents can follow the written workflow without relying on
  chat history.
- Priority and rationale: High workflow priority before the next product-domain
  task. The need was demonstrated by an actual evidence failure in PR #1, and
  the change reduces risk for every later financial feature.

## Knowledge classification

### Confirmed inputs

- Every task ships through a task branch and pull request.
- Claude Code owns implementation and opens the PR.
- Codex independently reviews the PR, records findings or acceptance in the
  product brief, and may push those completed review artifacts without asking
  each time.
- Codex may merge only after its Product Owner acceptance is committed and all
  required checks pass.
- Both agents currently authenticate to GitHub as `codelinguist`, so GitHub
  cannot provide an independent formal approval from Codex.
- The durable product brief, verified evidence, and merge commit are the
  authoritative approval trail; a fabricated or ineffective self-review is not.
- The current backend verification target is the complete Java 21 Maven suite,
  including PostgreSQL/Testcontainers integration tests.

### Product assumptions to validate

- GitHub Actions is available for this repository and can run the complete
  Testcontainers-backed suite.
- The repository's GitHub plan and settings support a required status check on
  `main`.
- One root-level executable command can hide local-versus-CI environment setup
  without weakening or skipping the PostgreSQL integration tests.
- A stable CI job/check name can be selected and required without blocking
  administrators from recovering a broken workflow configuration.

### Open questions

- Separate GitHub identities or a GitHub App may be useful later if formal
  platform-level reviewer identity becomes valuable; it is not required for
  this private household repository now.
- Additional security, UI, deployment, and risk-tier-specific checks remain
  follow-up work rather than part of this baseline delivery gate.

## Scope

### In scope

- A single executable verification command at the repository root.
- The complete existing Java 21 unit and PostgreSQL/Testcontainers integration
  suite behind that command, with no silent test exclusions.
- A GitHub Actions pull-request workflow that invokes the same command and
  exposes one stable, clearly named status check.
- Repository-level protection for `main` requiring that status check before
  merge, with read-back evidence of the applied setting.
- Revision of the existing pull-request template to reference the canonical
  command, automated check, task/product brief, applicable UI evidence, manual
  verification, deviations, and known limitations.
- Revision of `AGENTS.md` and `agent/collaboration-workflow.md` so Codex's
  standing authority to push completed review artifacts and its conditional
  merge authority are explicit and consistent.
- Documentation that same-account GitHub self-approval is unavailable and must
  not be represented as independent formal approval.
- Verification evidence and an implementation-log entry.

### Out of scope

- Application-domain behavior, database migrations, API changes, UI work, or
  canonical household financial data.
- Separate bot/GitHub accounts, GitHub Apps, paid services, or secrets.
- Deployment pipelines, preview environments, releases, rollback automation,
  dependency scanning, secret scanning, CodeQL, or a dedicated security lane.
- Multiple concurrent feature branches, worktree orchestration, autonomous
  backlog agents, or changes to the single-active-task convention.
- Risk-tier classification, workflow metrics, or property-based financial tests;
  these remain deliberate follow-up opportunities.
- Requiring a GitHub approving review that the current single GitHub identity
  cannot validly provide.

## User flow or behavior

1. Claude Code starts from the active task branch and runs the root verification
   command before opening or updating a PR.
2. Opening or updating the PR causes GitHub Actions to run that same command and
   publish the stable required check.
3. GitHub prevents merge while the check is missing, running, or failing.
4. Codex reviews the PR diff and evidence independently, records the result in
   the product brief, commits it, and pushes the review artifact.
5. If findings are returned, Claude addresses them on the same branch and CI
   reruns before Codex re-reviews.
6. After Codex commits `ACCEPTED` and required checks are green, Codex may merge
   the PR. Acceptance never substitutes for household approval of material
   financial-data or financial-policy changes.

## Acceptance criteria

- [ ] Running the documented root verification command on the supported local
  Docker environment executes the complete Java 21 Maven test suite, including
  PostgreSQL/Testcontainers tests, and exits nonzero on any failure.
- [ ] The verification command has no repository-location ambiguity and does
  not require an agent to reconstruct Docker mounts or working directories.
- [ ] A GitHub Actions workflow runs on pull requests targeting `main`, invokes
  the same root verification command, uses least-privilege permissions, and
  publishes one stable check name documented in the repository.
- [ ] The Task 003 PR demonstrates the check passing from a clean GitHub runner;
  the reported test count and result agree with local execution.
- [ ] `main` branch protection requires that exact status check; GitHub settings
  are read back after configuration and the evidence is recorded without
  storing credentials or tokens.
- [ ] The protection configuration does not require an impossible same-account
  approving review and retains a documented administrator recovery path for a
  broken CI definition.
- [ ] The pull-request template requires links to the active task and product
  brief, the canonical local verification result, the CI result, applicable UI
  evidence, manual-flow evidence, deviations, and known limitations.
- [ ] `AGENTS.md` and `agent/collaboration-workflow.md` consistently state that
  Codex may commit and push completed Product Owner review artifacts.
- [ ] Those documents consistently state that Codex may merge only after its
  acceptance commit is on the PR and every required check is green; neither
  agent may bypass a failed or missing required check.
- [ ] The workflow explicitly records that same-account Codex review cannot be
  represented as an independent GitHub approval; the product brief remains the
  durable acceptance record.
- [ ] Existing financial-domain behavior is unchanged and the full pre-existing
  test suite still passes.
- [ ] `agent/implementation-log.md` records commands, results, GitHub settings
  evidence, assumptions, limitations, and recommended follow-up work.

## Risks and safeguards

- Financial-data or household-approval boundary: This task changes delivery
  automation only. Product Owner acceptance and a green build never authorize a
  canonical financial-data write or material household financial decision.
- Privacy or sensitive-data considerations: CI must use representative test
  fixtures, must not upload local databases, and must not print GitHub tokens or
  household financial data.
- Accessibility considerations: No UI is in scope.
- Failure or misuse risks: A renamed check can deadlock merging; a weak script
  can create false confidence; administrator bypass can defeat the gate; and a
  same-account GitHub review can imply independence that does not exist. Use a
  stable documented check name, fail-fast command behavior, settings read-back,
  no routine bypass, and the product brief as the explicit acceptance record.

## Product decisions

### PD-001 — One verification entry point

- Decision: Local agents and GitHub Actions must invoke the same root-level
  verification command.
- Evidence: PR #1's first documented command failed because its working
  directory did not contain the POM even though the intended test suite passed.
- Alternatives considered: Duplicate raw Maven/Docker commands in docs and CI;
  leave command construction to each agent.
- Rationale: One executable contract removes command drift and makes evidence
  directly reproducible.
- User input required: `NO`

### PD-002 — CI is a required merge gate

- Decision: The canonical verification check is required on `main`; a missing,
  pending, or failed result blocks normal merge.
- Evidence: PR #1 had no configured status checks, so correctness depended only
  on prose and manual reruns.
- Alternatives considered: Advisory CI; manual verification only.
- Rationale: Deterministic financial software needs an objective repository
  boundary that neither implementation nor review prose can override.
- User input required: `NO`

### PD-003 — Preserve honest reviewer identity

- Decision: Do not require or claim independent GitHub approval while Claude
  and Codex share `codelinguist`; record Product Owner acceptance in the brief.
- Evidence: GitHub identifies both the PR author and authenticated reviewer as
  the same account and does not permit meaningful self-approval.
- Alternatives considered: Pretend a same-account review is independent; add a
  second identity now.
- Rationale: The repository artifact truthfully captures role separation
  without overstating platform identity guarantees.
- User input required: `NO`

### PD-004 — Codex may push reviews and conditionally merge

- Decision: Codex has standing authorization to commit and push completed
  Product Owner review artifacts and may merge a PR after its acceptance commit
  is present and all required checks are green.
- Evidence: Ralph explicitly established this workflow after Task 002.
- Alternatives considered: Ask before every review push; reserve every merge
  for Ralph or Claude; unconditional auto-merge.
- Rationale: This removes routine handoffs while preserving objective gates and
  household-approval boundaries.
- User input required: `NO`

## Delivery handoff

- Current task: `agent/current-task.md` — Task 003, Automate Delivery Gates
- Design brief, if applicable: Not applicable; no UI is in scope.
- Implementation owner: Claude Code
- Review evidence: PR #2 reviewed against all acceptance criteria. The recorded
  local run passed 62 tests with zero failures/errors; the clean GitHub runner
  passed the same 62 tests; and branch protection read-back requires exactly
  `verify`. A local rerun during review was not reproducible on this machine
  because it has Java 26 rather than the required Java 21 and no running Docker
  daemon; this is an environment limitation, not a repository failure.

## Feature acceptance

- Acceptance status: `ACCEPTED`
- Acceptance evidence: All twelve acceptance criteria are satisfied by the PR
  diff, implementation log, live GitHub Actions result, and branch-protection
  settings read-back. `git diff --check` is clean. No application-domain,
  API, schema, UI, or canonical household financial data was changed.
- Unmet criteria: None.
- Returned work: None.
- Follow-up opportunities: Risk-tiered gates, workflow metrics, security scans,
  separate reviewer identity, and parallel worktrees when concrete needs arise.
- Accepted or returned by Product Owner Agent: Accepted by Codex Product Owner
  Agent
- Accepted or returned at: 2026-09-03
