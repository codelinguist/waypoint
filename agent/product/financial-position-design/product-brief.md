# Product Brief: Financial Position Design Exploration

## Status

`READY`

## Ownership and user input

Codex owns product selection; Claude Code explores design. Created 2026-09-06 for Ralph's explicit request to explore and approve the first financial-position UI.

## Product framing and knowledge classification

The parent [financial-position brief](../financial-position/product-brief.md) is the authoritative user outcome and scope. Existing backend records support the content; no frontend, design system or household style preference is established. Layout selection is a reversible Product Owner decision, not an undiscoverable household financial preference.

## Scope and behavior

Use agent/templates/ui-design-brief.md to author agent/ui/financial-position/design-brief.md. Explore at least two materially different structures (for example, currency-first summaries with expandable holdings versus an inventory-first split view). Explain tradeoffs rather than just changing colors. Include synthetic static wireframes or browser-renderable disposable prototypes under agent/ui/financial-position/evidence/. These are design evidence, not production components or a frontend scaffold.

Show 390px and 1440px layouts, information hierarchy, source-date and planning-value explanations, refresh, no-data/loading/error/configuration states, and keyboard/narrow-layout behavior. Recommend one direction, then invoke Codex using the checked-in-artifact boundary in agent/collaboration-workflow.md. Record selected direction, amendments, rationale and explicit APPROVED status. Do not self-approve as implementer.

## Acceptance criteria

- [ ] At least two distinct directions have reviewable wide/narrow evidence and articulated tradeoffs against the parent outcome.
- [ ] All information, states and financial distinctions in the parent brief appear in the design specification, including large exact amounts, multiple currencies, negative net worth and long names.
- [ ] Interaction and accessibility behavior is concrete enough for a fresh implementation conversation; error/refresh retention behavior is chosen explicitly.
- [ ] Product Owner records the chosen direction, rationale, amendments and APPROVED status in the design brief.
- [ ] No production UI or backend code, dependencies or household data changes are introduced.
- [ ] Design PR and brief contain evidence and canonical ./verify.sh results; design-task acceptance is distinct from final UI acceptance.

## Risks and safeguards

Use synthetic data only. No financial facts/rules are created. No external hosting is needed. Design approval authorizes only the bounded implementation handoff, not a household financial decision.

## Product decision

Explore and approve separately from implementation to preserve the mandated fresh-conversation boundary. UI build task is withheld until this design and Task 021 are accepted. No extra Ralph approval is required for routine layout selection.

## Delivery handoff

- Task: agent/tasks/020-financial-position-design.md
- Ownership: agent/product/financial-position-design/** and agent/ui/financial-position/** only.
- Record commands, assumptions, decisions, limitations and system-evolution findings in the feature-local implementation-log.md; shared-prose consolidation is deferred.
- Follow-up: Queue the UI implementation using the parent implementation-task-draft.md only after both prerequisites merge. Choose the next unused task number at that time.

## Feature acceptance

- Acceptance status: PENDING
- Evidence: None yet.
- Unmet criteria: Exploration and recorded approval.
