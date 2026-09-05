#!/usr/bin/env bash
#
# Lightweight local regression tests for agent/automation/orchestrator.sh.
#
# Scope, deliberately: this sources orchestrator.sh (its `main "$@"` call is
# guarded so sourcing is safe -- see the bottom of that file) and exercises
# the functions that are pure local logic or operate on a disposable local
# git fixture: frontmatter helpers, prompt rendering, review-verdict
# classification, and Flyway migration-collision detection.
#
# Out of scope: anything that would require a real GitHub repo, a real
# `codex`/`claude` invocation, or a faithful mock of the `gh` CLI's JSON
# surface (dispatch_worker, run_review's actual `codex exec` call,
# try_merge's `gh pr checks`/`gh pr merge`, handle_blocking/handle_conflict
# end-to-end). Building that mock harness is real effort with a thin payoff
# for a single-household project; instead, this suite (a) unit-tests the
# pure decision logic those functions delegate to (determine_review_verdict,
# the same jq filter try_merge uses against canned check-list JSON) and (b)
# asserts the shape of the code paths that skip the mock boundary (e.g. that
# review_and_merge_phase's REVIEW_ERROR branch `continue`s before ever
# persisting review state), so a regression there still fails loudly.
#
# Run directly: agent/automation/tests/orchestrator_test.sh
set -uo pipefail

TESTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AUTOMATION_DIR="$(dirname "$TESTS_DIR")"
ORCHESTRATOR="$AUTOMATION_DIR/orchestrator.sh"

PASS=0
FAIL=0

fail() {
  FAIL=$((FAIL+1))
  echo "FAIL: $*"
}

pass() {
  PASS=$((PASS+1))
}

assert_equals() {
  local expected="$1" actual="$2" msg="$3"
  if [[ "$expected" == "$actual" ]]; then
    pass
  else
    fail "$msg (expected '$expected', got '$actual')"
  fi
}

assert_contains() {
  local haystack="$1" needle="$2" msg="$3"
  if [[ "$haystack" == *"$needle"* ]]; then
    pass
  else
    fail "$msg (expected to find '$needle')"
  fi
}

assert_not_contains() {
  local haystack="$1" needle="$2" msg="$3"
  if [[ "$haystack" != *"$needle"* ]]; then
    pass
  else
    fail "$msg (did not expect to find '$needle')"
  fi
}

assert_success() {
  # "$@" is a command; asserts it exits 0, without letting `set -e` (brought
  # in by sourcing orchestrator.sh below) kill the whole test run either way.
  local msg="$1"; shift
  if "$@" >/dev/null 2>&1; then
    pass
  else
    fail "$msg (expected exit 0, got $?)"
  fi
}

assert_failure() {
  local msg="$1"; shift
  if "$@" >/dev/null 2>&1; then
    fail "$msg (expected nonzero exit, got 0)"
  else
    pass
  fi
}

# ---- source the script under test -------------------------------------

# shellcheck source=/dev/null
source "$ORCHESTRATOR"

# Sourcing brought in orchestrator.sh's real LOG_FILE/STATE_DIR, which point
# at this repo's live agent/automation/{logs,state} -- the same files the
# real cron-driven pipeline reads and writes. Redirect them into a scratch
# directory so this test run never pollutes or races real orchestrator
# state.
TEST_TMP="$(mktemp -d)"
trap 'rm -rf "$TEST_TMP"' EXIT
LOG_FILE="$TEST_TMP/test-orchestrator.log"
STATE_DIR="$TEST_TMP/state"
mkdir -p "$STATE_DIR"

# ---- frontmatter helpers -------------------------------------------------

test_frontmatter_read_write() {
  local f="$TEST_TMP/task-a.md"
  cat > "$f" <<'EOF'
---
status: QUEUED
task_number: 099
feature_slug: example
branch: task/099-example
worktree:
session:
pr:
claimed_at:
fix_rounds: 0
conflict_rounds: 0
---

# Task 099: Example
EOF

  assert_equals "QUEUED" "$(frontmatter_field "$f" status)" \
    "frontmatter_field should read the initial status"

  set_frontmatter_field "$f" status IN_PROGRESS
  assert_equals "IN_PROGRESS" "$(frontmatter_field "$f" status)" \
    "set_frontmatter_field should update an existing field"

  set_frontmatter_field "$f" status IN_REVIEW
  set_frontmatter_field "$f" status MERGED
  assert_equals "MERGED" "$(frontmatter_field "$f" status)" \
    "frontmatter status should reflect the last transition"

  # Untouched fields survive an edit to a sibling field.
  assert_equals "099" "$(frontmatter_field "$f" task_number)" \
    "editing status should not disturb task_number"
}

test_set_frontmatter_field_adds_missing_field() {
  # Simulates an older task file authored before `conflict_rounds` existed
  # -- the exact scenario that would otherwise make handle_conflict's
  # counter silently stick at 0 forever (sed only rewrites an existing
  # line; see the comment on set_frontmatter_field).
  local f="$TEST_TMP/task-b.md"
  cat > "$f" <<'EOF'
---
status: IN_REVIEW
task_number: 012
feature_slug: legacy
branch: task/012-legacy
worktree: /tmp/wt
session: abc123
pr: 42
claimed_at: 2026-01-01T00:00:00Z
fix_rounds: 1
---

# Task 012: Legacy (no conflict_rounds field)
EOF

  assert_equals "" "$(frontmatter_field "$f" conflict_rounds)" \
    "a legacy file has no conflict_rounds line yet"

  set_frontmatter_field "$f" conflict_rounds 1
  assert_equals "1" "$(frontmatter_field "$f" conflict_rounds)" \
    "set_frontmatter_field should insert a missing field, not silently no-op"

  # The rest of the frontmatter, and the closing delimiter, must survive.
  assert_equals "IN_REVIEW" "$(frontmatter_field "$f" status)" \
    "inserting a missing field must not corrupt other frontmatter fields"
  local delimiter_count
  delimiter_count="$(grep -c '^---$' "$f")"
  assert_equals "2" "$delimiter_count" \
    "the file should still have exactly two frontmatter delimiters after insertion"
}

test_fix_rounds_and_conflict_rounds_are_independent() {
  local f="$TEST_TMP/task-c.md"
  cat > "$f" <<'EOF'
---
status: IN_REVIEW
task_number: 013
feature_slug: counters
branch: task/013-counters
worktree:
session:
pr: 7
claimed_at:
fix_rounds: 0
conflict_rounds: 0
---
EOF

  set_frontmatter_field "$f" fix_rounds 1
  assert_equals "1" "$(frontmatter_field "$f" fix_rounds)" "fix_rounds should update"
  assert_equals "0" "$(frontmatter_field "$f" conflict_rounds)" \
    "incrementing fix_rounds must not touch conflict_rounds"

  set_frontmatter_field "$f" conflict_rounds 1
  set_frontmatter_field "$f" conflict_rounds 2
  assert_equals "2" "$(frontmatter_field "$f" conflict_rounds)" "conflict_rounds should update independently"
  assert_equals "1" "$(frontmatter_field "$f" fix_rounds)" \
    "incrementing conflict_rounds must not reset or touch fix_rounds"
}

test_handle_blocking_and_handle_conflict_use_separate_fields() {
  # Source-shape regression check standing in for a full dispatch_worker
  # mock: handle_blocking must only read/write fix_rounds, and
  # handle_conflict must only read/write conflict_rounds, or the two retry
  # budgets would collapse back into a shared one.
  local blocking_body conflict_body
  blocking_body="$(awk '/^handle_blocking\(\) \{/,/^\}/' "$ORCHESTRATOR")"
  conflict_body="$(awk '/^handle_conflict\(\) \{/,/^\}/' "$ORCHESTRATOR")"

  assert_contains "$blocking_body" "fix_rounds" "handle_blocking should use fix_rounds"
  assert_not_contains "$blocking_body" "conflict_rounds" "handle_blocking should not touch conflict_rounds"
  assert_contains "$conflict_body" "conflict_rounds" "handle_conflict should use conflict_rounds"
  assert_not_contains "$conflict_body" "fix_rounds" "handle_conflict should not touch fix_rounds"
}

# ---- prompt rendering -----------------------------------------------------

test_render_review_prompt_no_duplicated_task_path() {
  local rendered
  rendered="$(render_review_prompt "5" "task/006-example" "agent/tasks/006-example.md" "example")"
  assert_contains "$rendered" "agent/tasks/006-example.md" \
    "rendered review prompt should reference the task file path"
  assert_not_contains "$rendered" "agent/tasks/agent/tasks/006-example.md" \
    "rendered review prompt must not double the agent/tasks/ prefix"
}

test_render_worker_prompt_no_duplicated_task_path() {
  local rendered
  rendered="$(render_worker_prompt "agent/tasks/006-example.md" "task/006-example" "")"
  assert_contains "$rendered" "agent/tasks/006-example.md" \
    "rendered worker prompt should reference the task file path"
  assert_not_contains "$rendered" "agent/tasks/agent/tasks/006-example.md" \
    "rendered worker prompt must not double the agent/tasks/ prefix"
}

# ---- review verdict classification ----------------------------------------

test_determine_review_verdict_accepted() {
  local mf="$TEST_TMP/msg-accepted.txt"
  printf 'Some review prose.\nREVIEW_VERDICT: ACCEPTED\n' > "$mf"
  assert_equals "ACCEPTED" "$(determine_review_verdict 1 0 "$mf")" \
    "a well-formed ACCEPTED message should classify as ACCEPTED"
}

test_determine_review_verdict_blocking() {
  local mf="$TEST_TMP/msg-blocking.txt"
  printf 'Some review prose.\nREVIEW_VERDICT: BLOCKING\n' > "$mf"
  assert_equals "BLOCKING" "$(determine_review_verdict 1 0 "$mf")" \
    "a well-formed BLOCKING message should classify as BLOCKING"
}

test_determine_review_verdict_malformed_is_review_error_not_blocking() {
  local mf="$TEST_TMP/msg-malformed.txt"
  printf 'Codex rambled without ever emitting a verdict line.\n' > "$mf"
  assert_equals "REVIEW_ERROR" "$(determine_review_verdict 1 0 "$mf")" \
    "a missing/unparseable verdict line must be REVIEW_ERROR, not BLOCKING (fail-safe was the bug)"
}

test_determine_review_verdict_missing_file_is_review_error() {
  assert_equals "REVIEW_ERROR" "$(determine_review_verdict 1 0 "$TEST_TMP/does-not-exist.txt")" \
    "a missing/empty message file must be REVIEW_ERROR"
}

test_determine_review_verdict_rejects_conflicting_lines_blocking_last() {
  # Regression for the exact defect Codex's own review of this PR found.
  # The old code (`grep -q ACCEPTED` checked, and returned, before ever
  # looking for BLOCKING) matched ACCEPTED's mere *presence* anywhere in
  # the message, independent of position -- so this ordering was every bit
  # as wrong under the old logic as the mirror case below: both returned
  # the wrong ACCEPTED verdict despite BLOCKING being the real final
  # decision. Neither order may authorize a merge now.
  local mf="$TEST_TMP/msg-conflict-blocking-last.txt"
  printf 'Earlier example:\nREVIEW_VERDICT: ACCEPTED\nFinal decision:\nREVIEW_VERDICT: BLOCKING\n' > "$mf"
  assert_equals "REVIEW_ERROR" "$(determine_review_verdict 1 0 "$mf")" \
    "multiple verdict lines (ACCEPTED then BLOCKING) must be REVIEW_ERROR, not resolved either way"
}

test_determine_review_verdict_rejects_conflicting_lines_accepted_last() {
  # The mirror ordering of the same defect above -- also wrongly ACCEPTED
  # under the old presence-based `grep -q ACCEPTED` check, for the same
  # reason (it never even looked at position or reached the BLOCKING elif).
  local mf="$TEST_TMP/msg-conflict-accepted-last.txt"
  printf 'REVIEW_VERDICT: BLOCKING\nOn reflection:\nREVIEW_VERDICT: ACCEPTED\n' > "$mf"
  assert_equals "REVIEW_ERROR" "$(determine_review_verdict 1 0 "$mf")" \
    "multiple verdict lines (BLOCKING then ACCEPTED) must be REVIEW_ERROR, never ACCEPTED"
}

test_determine_review_verdict_nonterminal_verdict_is_review_error() {
  # A single verdict-shaped line that isn't the message's actual last line
  # violates review-prompt.md's "nothing after it" contract and must not
  # authorize a merge just because grep found it somewhere.
  local mf="$TEST_TMP/msg-nonterminal.txt"
  printf 'REVIEW_VERDICT: ACCEPTED\nOops, one more thought after the verdict.\n' > "$mf"
  assert_equals "REVIEW_ERROR" "$(determine_review_verdict 1 0 "$mf")" \
    "a verdict line that isn't the final line must be REVIEW_ERROR, not ACCEPTED"
}

test_determine_review_verdict_nonzero_codex_status_is_review_error() {
  # Even if the message file happens to contain a leftover ACCEPTED line
  # (e.g. from a stale mktemp collision), a failed codex exec invocation
  # must never be trusted as a real verdict.
  local mf="$TEST_TMP/msg-stale-accepted.txt"
  printf 'REVIEW_VERDICT: ACCEPTED\n' > "$mf"
  assert_equals "REVIEW_ERROR" "$(determine_review_verdict 1 1 "$mf")" \
    "a nonzero codex exec exit status must always be REVIEW_ERROR, regardless of message content"
}

test_review_error_branch_continues_before_persisting_state() {
  # Full end-to-end coverage of review_and_merge_phase would need a mocked
  # `gh` and a live PR; this instead asserts the code shape that matters:
  # the REVIEW_ERROR branch must `continue` before the state file is
  # written or the ACCEPTED/BLOCKING case statement runs.
  local body snippet
  body="$(awk '/^review_and_merge_phase\(\) \{/,/^\}/' "$ORCHESTRATOR")"
  assert_contains "$body" 'REVIEW_ERROR' \
    "review_and_merge_phase should have a REVIEW_ERROR branch"
  snippet="$(printf '%s\n' "$body" | awk '/verdict" == "REVIEW_ERROR"/{f=1} f{print} f && /^      fi/{exit}')"
  assert_contains "$snippet" "continue" \
    "the REVIEW_ERROR branch must continue, never falling into the merge/fix-round case statement"
  # Check for the actual write action (redirecting into $state_file), not
  # just any mention of "last_reviewed_sha" -- the branch's own explanatory
  # comment mentions that name descriptively without writing it.
  assert_not_contains "$snippet" '> "$state_file"' \
    "the REVIEW_ERROR branch must not persist state to the PR state file"
}

test_run_review_uses_sandboxed_codex_invocation() {
  # Source-shape regression for the hardening change: run_review() must
  # invoke codex exec under its documented, verified sandbox
  # (-s workspace-write plus the network_access override -- confirmed
  # empirically that workspace-write alone leaves network off) and must
  # never again use the full --dangerously-bypass-approvals-and-sandbox
  # escape hatch.
  # Strip comment-only lines first -- the explanatory comment above the
  # invocation legitimately names the old flag by way of contrast, which
  # would otherwise make a plain substring check on the whole function
  # body false-positive (the same class of mistake caught earlier in this
  # file for a "last_reviewed_sha" comment).
  local body
  body="$(awk '/^run_review\(\) \{/,/^\}/' "$ORCHESTRATOR" | grep -v '^[[:space:]]*#')"
  assert_contains "$body" '-s workspace-write' \
    "run_review should invoke codex exec with -s workspace-write"
  assert_contains "$body" 'sandbox_workspace_write.network_access=true' \
    "run_review should explicitly enable network access under the sandbox (off by default under workspace-write)"
  assert_not_contains "$body" '--dangerously-bypass-approvals-and-sandbox' \
    "run_review must not use the full bypass any more"
}

# ---- gh-token cron fallback -----------------------------------------------
#
# Shared fake `gh` for every test below: never touches the real gh CLI or
# real credentials. Behavior is controlled entirely via env vars read at
# invocation time:
#   GH_AUTH_STATUS_EXIT   - exit code for `gh auth status` (default 0)
#   GH_FAKE_TOKEN         - value `gh auth token` prints and succeeds with;
#                           unset/empty makes `gh auth token` fail
#   GH_AUTH_TOKEN_MARKER  - if set, a marker file written whenever
#                           `gh auth token` is invoked at all, letting a
#                           test prove that call path was never reached
setup_fake_gh() {
  local fake_bin="$1"
  mkdir -p "$fake_bin"
  cat > "$fake_bin/gh" <<'EOS'
#!/usr/bin/env bash
case "$1 $2" in
  "auth status")
    exit "${GH_AUTH_STATUS_EXIT:-0}"
    ;;
  "auth token")
    [[ -n "${GH_AUTH_TOKEN_MARKER:-}" ]] && printf 'called' > "$GH_AUTH_TOKEN_MARKER"
    if [[ -n "${GH_FAKE_TOKEN:-}" ]]; then
      printf '%s' "$GH_FAKE_TOKEN"
      exit 0
    fi
    exit 1
    ;;
esac
exit 1
EOS
  chmod +x "$fake_bin/gh"
}

test_ensure_gh_token_caches_and_falls_back() {
  # Simulates the exact cron scenario: `gh auth token` succeeds once (an
  # interactive-ish run), caching the token; a later call where it fails
  # (the cron/Keychain gap) must fall back to that cache instead of
  # leaving GH_TOKEN unset. No override file involved -- pointed at a
  # path that provably doesn't exist, rather than relying on the real
  # machine happening not to have one.
  local fake_bin="$TEST_TMP/fake-gh-bin"
  setup_fake_gh "$fake_bin"

  local saved_path="$PATH" saved_cache="$GH_TOKEN_CACHE" saved_override="$GH_TOKEN_OVERRIDE_FILE"
  GH_TOKEN_CACHE="$TEST_TMP/gh-token-cache"
  rm -f "$GH_TOKEN_CACHE"
  GH_TOKEN_OVERRIDE_FILE="$TEST_TMP/gh-token.scoped.absent-for-this-test"
  rm -f "$GH_TOKEN_OVERRIDE_FILE"
  PATH="$fake_bin:$PATH"

  local rc=0
  GH_FAKE_TOKEN="tok-fresh" ensure_gh_token || rc=$?
  assert_equals "0" "$rc" "ensure_gh_token should return 0 on a fresh live token"
  assert_equals "tok-fresh" "${GH_TOKEN:-}" \
    "ensure_gh_token should export a freshly obtained token"
  assert_equals "tok-fresh" "$(cat "$GH_TOKEN_CACHE" 2>/dev/null || true)" \
    "a freshly obtained token should be cached to disk"

  # cron scenario: gh auth token now fails, but the earlier cache exists.
  unset GH_TOKEN
  rc=0
  GH_FAKE_TOKEN="" ensure_gh_token || rc=$?
  assert_equals "0" "$rc" "ensure_gh_token should return 0 when falling back to a valid cache"
  assert_equals "tok-fresh" "${GH_TOKEN:-}" \
    "ensure_gh_token should fall back to the cached token when gh auth token fails"

  PATH="$saved_path"
  GH_TOKEN_CACHE="$saved_cache"
  GH_TOKEN_OVERRIDE_FILE="$saved_override"
  unset GH_TOKEN
}

test_ensure_gh_token_no_cache_no_token_leaves_gh_token_unset() {
  local fake_bin="$TEST_TMP/fake-gh-bin-empty"
  setup_fake_gh "$fake_bin"

  local saved_path="$PATH" saved_cache="$GH_TOKEN_CACHE" saved_override="$GH_TOKEN_OVERRIDE_FILE"
  GH_TOKEN_CACHE="$TEST_TMP/gh-token-cache-missing"
  rm -f "$GH_TOKEN_CACHE"
  GH_TOKEN_OVERRIDE_FILE="$TEST_TMP/gh-token.scoped.does-not-exist"
  rm -f "$GH_TOKEN_OVERRIDE_FILE"
  PATH="$fake_bin:$PATH"

  # A stale/inherited GH_TOKEN (a leftover export from earlier in this
  # shell, or something the invoking environment set) must not survive
  # this call just because no better credential was found.
  GH_TOKEN="stale-inherited-token"
  local rc=0
  GH_FAKE_TOKEN="" ensure_gh_token || rc=$?

  assert_equals "1" "$rc" \
    "with no live token, no cache, and no override, ensure_gh_token should return 1"
  assert_equals "" "${GH_TOKEN:-}" \
    "with no usable credential, ensure_gh_token must leave GH_TOKEN unset -- including clearing an inherited value"

  PATH="$saved_path"
  GH_TOKEN_CACHE="$saved_cache"
  GH_TOKEN_OVERRIDE_FILE="$saved_override"
  unset GH_TOKEN
}

test_ensure_gh_token_rejects_invalid_cached_broad_token() {
  local fake_bin="$TEST_TMP/fake-gh-invalid-cache"
  setup_fake_gh "$fake_bin"
  local saved_path="$PATH" saved_override="$GH_TOKEN_OVERRIDE_FILE" saved_cache="$GH_TOKEN_CACHE"
  PATH="$fake_bin:$PATH"
  GH_TOKEN_OVERRIDE_FILE="$TEST_TMP/gh-token.scoped.absent-invalid-cache"
  rm -f "$GH_TOKEN_OVERRIDE_FILE"
  GH_TOKEN_CACHE="$TEST_TMP/gh-token-cache-invalid"
  printf 'revoked-broad-token' > "$GH_TOKEN_CACHE"

  GH_TOKEN="stale-inherited-token"
  local rc=0
  GH_FAKE_TOKEN="" GH_AUTH_STATUS_EXIT=1 ensure_gh_token || rc=$?

  assert_equals "1" "$rc" "a cached broad token that fails validation should return 1"
  assert_equals "" "${GH_TOKEN:-}" "an invalid cached broad token must leave GH_TOKEN unset"

  PATH="$saved_path"
  GH_TOKEN_OVERRIDE_FILE="$saved_override"
  GH_TOKEN_CACHE="$saved_cache"
  unset GH_TOKEN
}

test_ensure_gh_token_valid_override_short_circuits_broad_path() {
  local fake_bin="$TEST_TMP/fake-gh-override-valid"
  setup_fake_gh "$fake_bin"
  local saved_path="$PATH" saved_override="$GH_TOKEN_OVERRIDE_FILE" saved_cache="$GH_TOKEN_CACHE"
  PATH="$fake_bin:$PATH"

  GH_TOKEN_OVERRIDE_FILE="$TEST_TMP/gh-token.scoped.valid"
  printf 'scoped-token-value\n' > "$GH_TOKEN_OVERRIDE_FILE"
  chmod 600 "$GH_TOKEN_OVERRIDE_FILE"
  GH_TOKEN_CACHE="$TEST_TMP/gh-token-cache-unused-by-override"
  rm -f "$GH_TOKEN_CACHE"

  local marker="$TEST_TMP/gh-auth-token-called.valid-override"
  rm -f "$marker"
  unset GH_TOKEN
  local rc=0
  GH_AUTH_TOKEN_MARKER="$marker" ensure_gh_token || rc=$?

  assert_equals "0" "$rc" "a valid override should make ensure_gh_token return 0"
  assert_equals "scoped-token-value" "${GH_TOKEN:-}" \
    "ensure_gh_token should export the override file's (trimmed) content"
  [[ ! -e "$marker" ]] && pass || fail "a valid override must never invoke 'gh auth token' (the broad-scope path), even though it legitimately calls 'gh auth status' to validate itself"

  PATH="$saved_path"
  GH_TOKEN_OVERRIDE_FILE="$saved_override"
  GH_TOKEN_CACHE="$saved_cache"
  unset GH_TOKEN
}

test_ensure_gh_token_override_symlink_rejected() {
  local fake_bin="$TEST_TMP/fake-gh-override-symlink"
  setup_fake_gh "$fake_bin"
  local saved_path="$PATH" saved_override="$GH_TOKEN_OVERRIDE_FILE" saved_cache="$GH_TOKEN_CACHE"
  PATH="$fake_bin:$PATH"

  local real_file="$TEST_TMP/gh-token.scoped.real-target"
  printf 'sneaky-token' > "$real_file"
  chmod 600 "$real_file"
  GH_TOKEN_OVERRIDE_FILE="$TEST_TMP/gh-token.scoped.symlink"
  ln -sf "$real_file" "$GH_TOKEN_OVERRIDE_FILE"
  GH_TOKEN_CACHE="$TEST_TMP/gh-token-cache-unused.symlink"
  rm -f "$GH_TOKEN_CACHE"

  local marker="$TEST_TMP/gh-auth-token-called.symlink"
  rm -f "$marker"
  GH_TOKEN="stale-inherited-token"
  local rc=0
  GH_AUTH_TOKEN_MARKER="$marker" ensure_gh_token || rc=$?

  assert_equals "1" "$rc" "a symlinked override file should be rejected (return 1)"
  assert_equals "" "${GH_TOKEN:-}" "a rejected symlink override must leave GH_TOKEN unset, including clearing an inherited value"
  [[ ! -e "$marker" ]] && pass || fail "a rejected symlink override must never invoke 'gh auth token'"

  PATH="$saved_path"
  GH_TOKEN_OVERRIDE_FILE="$saved_override"
  GH_TOKEN_CACHE="$saved_cache"
  unset GH_TOKEN
}

test_ensure_gh_token_override_bad_permissions_rejected() {
  local fake_bin="$TEST_TMP/fake-gh-override-badperm"
  setup_fake_gh "$fake_bin"
  local saved_path="$PATH" saved_override="$GH_TOKEN_OVERRIDE_FILE" saved_cache="$GH_TOKEN_CACHE"
  PATH="$fake_bin:$PATH"

  GH_TOKEN_OVERRIDE_FILE="$TEST_TMP/gh-token.scoped.badperm"
  printf 'some-token' > "$GH_TOKEN_OVERRIDE_FILE"
  chmod 644 "$GH_TOKEN_OVERRIDE_FILE"
  GH_TOKEN_CACHE="$TEST_TMP/gh-token-cache-unused.badperm"
  rm -f "$GH_TOKEN_CACHE"

  local marker="$TEST_TMP/gh-auth-token-called.badperm"
  rm -f "$marker"
  GH_TOKEN="stale-inherited-token"
  local rc=0
  GH_AUTH_TOKEN_MARKER="$marker" ensure_gh_token || rc=$?

  assert_equals "1" "$rc" "a group/world-readable override file should be rejected (return 1)"
  assert_equals "" "${GH_TOKEN:-}" "a rejected bad-permission override must leave GH_TOKEN unset"
  [[ ! -e "$marker" ]] && pass || fail "a rejected bad-permission override must never invoke 'gh auth token'"

  PATH="$saved_path"
  GH_TOKEN_OVERRIDE_FILE="$saved_override"
  GH_TOKEN_CACHE="$saved_cache"
  unset GH_TOKEN
}

test_ensure_gh_token_override_empty_rejected() {
  local fake_bin="$TEST_TMP/fake-gh-override-empty"
  setup_fake_gh "$fake_bin"
  local saved_path="$PATH" saved_override="$GH_TOKEN_OVERRIDE_FILE" saved_cache="$GH_TOKEN_CACHE"
  PATH="$fake_bin:$PATH"

  GH_TOKEN_OVERRIDE_FILE="$TEST_TMP/gh-token.scoped.empty"
  printf '   \n\n' > "$GH_TOKEN_OVERRIDE_FILE"
  chmod 600 "$GH_TOKEN_OVERRIDE_FILE"
  GH_TOKEN_CACHE="$TEST_TMP/gh-token-cache-unused.empty"
  rm -f "$GH_TOKEN_CACHE"

  local marker="$TEST_TMP/gh-auth-token-called.empty"
  rm -f "$marker"
  GH_TOKEN="stale-inherited-token"
  local rc=0
  GH_AUTH_TOKEN_MARKER="$marker" ensure_gh_token || rc=$?

  assert_equals "1" "$rc" "a whitespace-only override file should be rejected (return 1)"
  assert_equals "" "${GH_TOKEN:-}" "a rejected empty override must leave GH_TOKEN unset"
  [[ ! -e "$marker" ]] && pass || fail "a rejected empty override must never invoke 'gh auth token'"

  PATH="$saved_path"
  GH_TOKEN_OVERRIDE_FILE="$saved_override"
  GH_TOKEN_CACHE="$saved_cache"
  unset GH_TOKEN
}

test_ensure_gh_token_override_failed_validation_rejected_without_cache_fallback() {
  # The specific "fail visibly, don't silently widen scope" case: even
  # with a valid cached broad token sitting right there, a scoped override
  # that fails gh auth status validation (revoked/expired/malformed) must
  # not fall back to it.
  local fake_bin="$TEST_TMP/fake-gh-override-invalid-status"
  setup_fake_gh "$fake_bin"
  local saved_path="$PATH" saved_override="$GH_TOKEN_OVERRIDE_FILE" saved_cache="$GH_TOKEN_CACHE"
  PATH="$fake_bin:$PATH"

  GH_TOKEN_OVERRIDE_FILE="$TEST_TMP/gh-token.scoped.badstatus"
  printf 'revoked-token' > "$GH_TOKEN_OVERRIDE_FILE"
  chmod 600 "$GH_TOKEN_OVERRIDE_FILE"
  GH_TOKEN_CACHE="$TEST_TMP/gh-token-cache-present.badstatus"
  printf 'broad-cached-token' > "$GH_TOKEN_CACHE"

  local marker="$TEST_TMP/gh-auth-token-called.badstatus"
  rm -f "$marker"
  GH_TOKEN="stale-inherited-token"
  local rc=0
  GH_AUTH_STATUS_EXIT=1 GH_AUTH_TOKEN_MARKER="$marker" ensure_gh_token || rc=$?

  assert_equals "1" "$rc" "an override that fails gh auth status validation should return 1"
  assert_equals "" "${GH_TOKEN:-}" \
    "a failed-validation override must leave GH_TOKEN unset, not silently fall back to the cached broad token"
  [[ ! -e "$marker" ]] && pass || fail "a failed-validation override must never invoke 'gh auth token' (the broad-scope fallback)"

  PATH="$saved_path"
  GH_TOKEN_OVERRIDE_FILE="$saved_override"
  GH_TOKEN_CACHE="$saved_cache"
  unset GH_TOKEN
}

test_authenticated_url_for_with_and_without_token() {
  local saved_token="${GH_TOKEN:-}"

  GH_TOKEN="tok-abc"
  assert_equals "https://x-access-token:tok-abc@github.com/x/y.git" \
    "$(authenticated_url_for "https://github.com/x/y.git")" \
    "authenticated_url_for should embed GH_TOKEN when set"

  unset GH_TOKEN
  assert_equals "https://github.com/x/y.git" \
    "$(authenticated_url_for "https://github.com/x/y.git")" \
    "authenticated_url_for should pass the URL through unchanged when GH_TOKEN is unset"

  if [[ -n "$saved_token" ]]; then GH_TOKEN="$saved_token"; else unset GH_TOKEN; fi
}

test_sync_control_clone_authenticates_the_first_clone() {
  # Executing this path requires a real private remote, so protect its key
  # ordering property with a source-shape regression instead.
  local body
  body="$(awk '/^sync_control_clone\(\) \{/,/^\}/' "$ORCHESTRATOR" | grep -v '^[[:space:]]*#')"
  assert_contains "$body" 'git clone --quiet "$(authenticated_url_for "$origin_url")" "$CONTROL_DIR"' \
    "sync_control_clone should authenticate the first-ever private-repository clone"
}

test_ensure_authenticated_remote_scrubs_stale_token() {
  # Real (but disposable, local, unreachable) git repos standing in for
  # $REPO_ROOT and $CONTROL_DIR -- no network needed, `remote add`/
  # `get-url`/`set-url` are all purely local operations.
  local repo_root_stub="$TEST_TMP/repo-root-stub"
  local control_stub="$TEST_TMP/control-stub"
  mkdir -p "$repo_root_stub" "$control_stub"
  git -C "$repo_root_stub" init --quiet
  git -C "$repo_root_stub" remote add origin "https://github.com/codelinguist/waypoint.git"
  git -C "$control_stub" init --quiet
  git -C "$control_stub" remote add origin "https://x-access-token:OLD-STALE-TOKEN@github.com/codelinguist/waypoint.git"

  local saved_repo_root="$REPO_ROOT" saved_control_dir="$CONTROL_DIR"
  REPO_ROOT="$repo_root_stub"
  CONTROL_DIR="$control_stub"

  unset GH_TOKEN
  ensure_authenticated_remote

  local resulting_url
  resulting_url="$(git -C "$control_stub" remote get-url origin)"
  assert_equals "https://github.com/codelinguist/waypoint.git" "$resulting_url" \
    "ensure_authenticated_remote must scrub a previously-embedded token when GH_TOKEN is unset, not leave a prior run's credential in place"

  REPO_ROOT="$saved_repo_root"
  CONTROL_DIR="$saved_control_dir"
}

test_main_aborts_when_no_usable_credential() {
  # The central new guarantee: when ensure_gh_token fails, main() must
  # stop before sync_control_clone/review_and_merge_phase/dispatch_phase
  # are ever called -- not just hope individual git/gh calls inside them
  # fail safely on their own. Stubs everything main() calls other than
  # ensure_gh_token itself (bash allows redefining sourced functions at
  # runtime), so this exercises main()'s actual control flow with no real
  # locking, cloning, or GitHub/Codex/Claude calls.
  local marker_dir="$TEST_TMP/main-abort-markers"
  mkdir -p "$marker_dir"

  require_tools() { :; }
  acquire_lock() { :; }
  sync_control_clone() { touch "$marker_dir/sync_control_clone"; }
  review_and_merge_phase() { touch "$marker_dir/review_and_merge_phase"; }
  dispatch_phase() { touch "$marker_dir/dispatch_phase"; }

  local saved_override="$GH_TOKEN_OVERRIDE_FILE" saved_cache="$GH_TOKEN_CACHE"
  GH_TOKEN_OVERRIDE_FILE="$TEST_TMP/gh-token.scoped.for-main-abort"
  printf 'irrelevant' > "$GH_TOKEN_OVERRIDE_FILE"
  chmod 644 "$GH_TOKEN_OVERRIDE_FILE"   # wrong permissions -> ensure_gh_token rejects it
  GH_TOKEN_CACHE="$TEST_TMP/gh-token-cache-for-main-abort"
  rm -f "$GH_TOKEN_CACHE"

  unset GH_TOKEN
  local exit_status=0
  main || exit_status=$?

  assert_equals "0" "$exit_status" \
    "main() should exit 0 (a handled no-op tick), not crash, when no credential is available"
  assert_equals "" "${GH_TOKEN:-}" "GH_TOKEN must remain unset after main() aborts"
  [[ ! -e "$marker_dir/sync_control_clone" ]] && pass || fail "main() must not call sync_control_clone when ensure_gh_token fails"
  [[ ! -e "$marker_dir/review_and_merge_phase" ]] && pass || fail "main() must not call review_and_merge_phase when ensure_gh_token fails"
  [[ ! -e "$marker_dir/dispatch_phase" ]] && pass || fail "main() must not call dispatch_phase when ensure_gh_token fails"

  GH_TOKEN_OVERRIDE_FILE="$saved_override"
  GH_TOKEN_CACHE="$saved_cache"
  unset GH_TOKEN

  # Restore the real function definitions (re-sourcing also resets
  # top-level vars to their production values, so re-apply this file's
  # log/state redirection immediately after).
  source "$ORCHESTRATOR"
  LOG_FILE="$TEST_TMP/test-orchestrator.log"
  STATE_DIR="$TEST_TMP/state"
}

# ---- try_merge's verify-check classification (jq filter only) -------------
#
# try_merge derives $check_state from `gh pr checks ... --jq '<filter>'`.
# Reproducing gh itself is out of scope (see file header); this instead
# feeds the exact same filter the same canned JSON gh would return, so a
# regression in the filter's MISSING/PENDING/SUCCESS handling still fails.

VERIFY_CHECK_JQ_FILTER='(.[] | select(.name=="verify") | .state) // "MISSING"'

test_verify_check_missing_when_no_verify_entry() {
  local state
  state="$(echo '[]' | jq -r "$VERIFY_CHECK_JQ_FILTER")"
  assert_equals "MISSING" "$state" "an empty checks list should classify as MISSING"

  state="$(echo '[{"name":"other","state":"SUCCESS"}]' | jq -r "$VERIFY_CHECK_JQ_FILTER")"
  assert_equals "MISSING" "$state" "a checks list without a 'verify' entry should classify as MISSING"
}

test_verify_check_pending() {
  local state
  state="$(echo '[{"name":"verify","state":"PENDING"}]' | jq -r "$VERIFY_CHECK_JQ_FILTER")"
  assert_equals "PENDING" "$state" "an in-flight verify check should classify as PENDING"
}

test_verify_check_success() {
  local state
  state="$(echo '[{"name":"verify","state":"SUCCESS"}]' | jq -r "$VERIFY_CHECK_JQ_FILTER")"
  assert_equals "SUCCESS" "$state" "a green verify check should classify as SUCCESS"
}

# ---- Flyway migration collision detection ---------------------------------

setup_migration_fixture() {
  local root="$TEST_TMP/migration-fixture"
  local origin="$root/origin.git"
  local seed="$root/seed"
  local mig="backend/src/main/resources/db/migration"
  mkdir -p "$root"

  git init --quiet --bare -b main "$origin"
  git init --quiet -b main "$seed"
  git -C "$seed" config user.email "test@example.com"
  git -C "$seed" config user.name "Test"
  git -C "$seed" remote add origin "$origin"

  mkdir -p "$seed/$mig"
  echo "-- v1" > "$seed/$mig/V1__init.sql"
  git -C "$seed" add -A
  git -C "$seed" commit --quiet -m "V1"
  git -C "$seed" push --quiet origin main

  # task/no-collision: adds a brand-new version main never claims.
  git -C "$seed" checkout --quiet -b task/no-collision main
  echo "-- v3" > "$seed/$mig/V3__later.sql"
  git -C "$seed" add -A
  git -C "$seed" commit --quiet -m "V3 on branch"
  git -C "$seed" push --quiet origin task/no-collision

  # task/collision: adds V2 with one filename/content...
  git -C "$seed" checkout --quiet -b task/collision main
  echo "-- v2 from branch" > "$seed/$mig/V2__branch_version.sql"
  git -C "$seed" add -A
  git -C "$seed" commit --quiet -m "V2 on branch"
  git -C "$seed" push --quiet origin task/collision

  # ...while main independently gains a different V2 in the meantime (the
  # "sibling task already merged" scenario).
  git -C "$seed" checkout --quiet main
  echo "-- v2 from main" > "$seed/$mig/V2__main_version.sql"
  git -C "$seed" add -A
  git -C "$seed" commit --quiet -m "V2 on main (sibling merge)"
  git -C "$seed" push --quiet origin main

  # task/duplicate: a single branch that claims V4 twice within itself.
  git -C "$seed" checkout --quiet -b task/duplicate main
  echo "-- v4 a" > "$seed/$mig/V4__first.sql"
  echo "-- v4 b" > "$seed/$mig/V4__second.sql"
  git -C "$seed" add -A
  git -C "$seed" commit --quiet -m "V4 twice"
  git -C "$seed" push --quiet origin task/duplicate

  git clone --quiet "$origin" "$root/control"
  echo "$root/control"
}

test_migration_collision_detection() {
  local control
  control="$(setup_migration_fixture)"
  local saved_control_dir="$CONTROL_DIR"
  CONTROL_DIR="$control"

  assert_success "a branch introducing a genuinely new version should not collide" \
    check_migration_collision "task/no-collision"

  assert_failure "two branches independently claiming the same Flyway version should collide" \
    check_migration_collision "task/collision"

  assert_failure "a branch claiming the same version twice within itself should collide" \
    check_migration_collision "task/duplicate"

  CONTROL_DIR="$saved_control_dir"
}

# ---- run everything ---------------------------------------------------

for t in $(declare -F | awk '{print $3}' | grep '^test_'); do
  "$t"
done

echo
echo "orchestrator_test.sh: $PASS passed, $FAIL failed"
[[ "$FAIL" -eq 0 ]]
