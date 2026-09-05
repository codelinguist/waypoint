# Implementation Plan: <Feature Name>

## Status

`DRAFT` | `READY` | `IN_PROGRESS` | `DONE`

## When to use this

Optional. Write this at the start of step 4 (Implement), before writing any
code, only when the task touches enough files, migrations, or existing
behavior that a plain read of the brief leaves real ambiguity about where
things go. Skip it for small, well-bounded tasks — the brief is enough on its
own.

## Source

- Product brief: `agent/product/<feature-slug>/product-brief.md`
- Design brief, if applicable: `agent/ui/<feature-slug>/design-brief.md`
- Current task: `agent/tasks/<NNN>-<feature-slug>.md`

## Codebase patterns to follow

- Pattern to mirror: `<path:line-range>` — why:
- Naming or convention to preserve: `<path:line-range>` — why:
- Anti-pattern to avoid:

## Files

### To modify

- `path/to/file` — what changes and why

### To create

- `path/to/new/file` — what it's for

## Task list

Ordered, dependency-first, top to bottom. Each task names its own validation
command so correctness is checked as work proceeds, not only at the end.

1. `CREATE` | `UPDATE` | `ADD` | `REMOVE` — `path/to/file`
   - Detail:
   - Validate: `<executable command>`

## Validation strategy

Defined here before writing code, per the Plan/Implement/Validate discipline
in `agent/collaboration-workflow.md`.

- Unit tests:
- Integration tests:
- Manual flow to exercise:
- Canonical check: `./verify.sh`

## Confidence

- One-pass confidence (1-10):
- Main risk if this implementation goes wrong:
