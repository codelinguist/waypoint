# Review and Accept

The Product Owner Agent independently reviews without editing implementation
code. Read the Jira
issue, current PR diff and revision, affected code/tests, current-round
findings, and available evidence. Scan `docs/decisions/index.md`; open only
matching decision bodies or other product/domain/architecture material needed
by a criterion or concrete finding.

For repeat review, inspect the delta from the previously reviewed revision and
reopen unchanged evidence only when that delta or an unresolved finding needs
it. Review from the main clone without changing its checkout: use `gh pr diff`
and targeted `git show origin/<branch>:<path>` or `gh api`. Do not rerun the app;
verification belongs to Implement. Use
`agent/templates/ui-visual-review.md` for UI evidence.

Record BLOCKING, RECOMMENDED, or OPTIONAL findings and acceptance conditions on
the Jira issue. ACCEPT only when every criterion has evidence; record the exact
revision and move the issue to Acceptance. Otherwise record RETURNED and move
it to In Progress. Acceptance does not authorize fixes, canonical household
data changes, or merge. Because implementation and review agents share a GitHub
account, the Jira record—not a same-account GitHub approval—is the independent
acceptance record. When the reviewing agent is the same agent that implemented
this revision, delegate the actual review read above to a fresh subagent with
no memory of the implementation, and record its verdict rather than
self-certifying from the implementing session (D020). Report the verdict and
stop.
