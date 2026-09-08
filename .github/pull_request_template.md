## Task

- Jira issue: `<issue-key>` — <one-line title>
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

Reviewed by the Product Owner Agent (Codex) against the linked Jira issue.
Findings are recorded as comments on the Jira issue (and `visual-review.md`
for UI tasks), not as PR comments. Because Claude Code and Codex currently
authenticate to GitHub as the same account, this review is not an
independent formal GitHub approval — the Jira issue's recorded acceptance
comment is the durable approval record. Merge only after the issue is marked
accepted and the required `verify` check is green; neither agent merges past
a failed or missing required check. Shipping also requires an explicit user
request; acceptance and CI do not trigger a merge.
