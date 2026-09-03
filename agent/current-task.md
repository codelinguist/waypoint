# Current Task

## Task 003 — Automate Delivery Gates

### Goal

Turn the proven Claude implementation -> Codex review -> acceptance -> merge
workflow into a reproducible, enforced repository contract.

Detailed requirements and acceptance criteria:

- `agent/product/delivery-gates/product-brief.md`

### Outcome

Every pull request targeting `main` runs one canonical repository verification
command in GitHub Actions, GitHub requires that check before merge, and the
written workflow clearly authorizes Codex to push review artifacts and merge
only accepted, green pull requests.

### Required deliverables

1. Add one executable verification command at the repository root.
2. Ensure it runs the complete Java 21 Maven suite, including the existing
   PostgreSQL/Testcontainers integration tests, in the supported local Docker
   environment.
3. Add a least-privilege GitHub Actions workflow for pull requests to `main`
   that invokes the same command and exposes a stable, documented check name.
4. Configure `main` branch protection to require that exact check and read the
   setting back as reviewable evidence.
5. Revise the existing pull-request template to require the canonical local
   result, CI result, task/product links, applicable UI evidence, manual-flow
   evidence, deviations, and known limitations.
6. Update `AGENTS.md` and `agent/collaboration-workflow.md` consistently:
   - Codex may commit and push completed Product Owner review artifacts without
     asking each time.
   - Codex may merge only after its acceptance commit is included in the PR and
     every required check is green.
   - Neither agent may bypass failed or missing required checks.
   - Same-account Codex review is not an independent formal GitHub approval;
     the product brief is the durable acceptance record.
7. Update `agent/implementation-log.md` with verification and GitHub-settings
   evidence.

### Constraints

- Do not change application-domain behavior, API contracts, database schemas,
  UI, or canonical household financial data.
- Do not add separate GitHub identities, paid services, secrets, deployment
  pipelines, security scanners, preview environments, risk tiers, workflow
  metrics, worktree orchestration, or autonomous agent swarms.
- Do not weaken, skip, or conditionally exclude the PostgreSQL/Testcontainers
  integration tests.
- Use representative fixtures only; do not upload databases or expose tokens.
- Preserve one active task branch and one implementation owner.
- Branch: `task/003-delivery-gates`.

### Definition of Done

- every acceptance criterion in the linked product brief is satisfied
- the canonical verification command passes locally and fails when a test fails
- the Task 003 PR reports the same command passing on a clean GitHub runner
- `main` requires the stable CI check, confirmed through settings read-back
- the PR template and both workflow-policy documents agree
- the full pre-existing test suite remains green
- implementation evidence is recorded for independent Product Owner review
- Claude pushes the branch and opens the PR; Codex accepts and merges only after
  the required check is green
