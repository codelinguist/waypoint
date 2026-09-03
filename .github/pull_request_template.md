## Task

- Task: `agent/current-task.md` — Task <NNN>, <task name>
- Product brief: `agent/product/<feature-slug>/product-brief.md`
- Design brief (UI tasks only): `agent/ui/<feature-slug>/design-brief.md`

## Checks run

- [ ] Canonical verification run locally: `./verify.sh` — result: `<PASS/FAIL,
      test count>`
- [ ] Required CI check `verify` — result: `<PASS/FAIL>`, run:
      `<GitHub Actions run URL>`
- [ ] Primary user flow exercised manually against the running application —
      `<how, e.g. curl/UI steps and observed result>`
- [ ] (UI tasks only) Wide and narrow layouts rendered — evidence in
      `agent/ui/<feature-slug>/evidence/`

## Deviations and known limitations

-

## Review

Reviewed by the Product Owner Agent (Codex) against the linked product brief.
Findings are recorded in the product brief's acceptance section (and
`visual-review.md` for UI tasks), not as PR comments. Because Claude Code and
Codex currently authenticate to GitHub as the same account, this review is
not an independent formal GitHub approval — the product brief's recorded
acceptance is the durable approval record. Merge only after the brief is
marked `ACCEPTED` and the required `verify` check is green; neither agent
merges past a failed or missing required check.
