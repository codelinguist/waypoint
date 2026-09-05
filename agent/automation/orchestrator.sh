#!/usr/bin/env bash
#
# Automated Plan/Implement/Validate pipeline, per
# agent/collaboration-workflow.md -> "Automated pipeline".
#
# Runs one pass: claim QUEUED tasks (bounded concurrency), review open PRs
# with Codex, and merge accepted ones. Meant to be invoked repeatedly on a
# schedule (cron); it is not itself a loop. Safe to run with nothing queued —
# it just does nothing and exits.
#
# Everything this script touches lives outside the interactive working
# copy at $REPO_ROOT: it keeps its own clone ("the control clone") and its
# own git worktrees under $ORCHESTRATOR_HOME, so it never mutates whatever
# branch a human happens to have checked out in $REPO_ROOT.
set -euo pipefail

# cron invokes this with a minimal PATH (typically just /usr/bin:/bin) that
# doesn't include Homebrew, anaconda, or user-local install locations --
# require_tools() below would otherwise fail to find gh/claude/codex/jq
# even though they work fine when this script is run by hand from an
# interactive shell. Confirmed empirically: cron got past a separate Full
# Disk Access denial only to fail here next, with none of this visible
# except in orchestrator.log.
export PATH="/opt/homebrew/bin:/opt/anaconda3/bin:/usr/local/bin:$HOME/.local/bin:/usr/bin:/bin:/usr/sbin:/sbin:$PATH"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
AUTOMATION_DIR="$REPO_ROOT/agent/automation"
ORCHESTRATOR_HOME="$(dirname "$REPO_ROOT")/waypoint-orchestrator"
CONTROL_DIR="$ORCHESTRATOR_HOME/control"
WORKTREE_ROOT="$ORCHESTRATOR_HOME/worktrees"
STATE_DIR="$AUTOMATION_DIR/state"
LOG_DIR="$AUTOMATION_DIR/logs"
LOG_FILE="$LOG_DIR/orchestrator.log"
LOCK_DIR="$STATE_DIR/.orchestrator.lock"

MAX_PARALLEL="${WAYPOINT_MAX_PARALLEL:-3}"
# Kept as two independent budgets, each with its own frontmatter field
# (fix_rounds / conflict_rounds) and its own counter, even though they
# default to the same number: a PR that burns through fix rounds on a
# genuine product-review finding and then hits an unrelated sibling-merge
# conflict (or vice versa) must not find its budget already spent by the
# other kind of failure.
MAX_FIX_ROUNDS="${WAYPOINT_MAX_FIX_ROUNDS:-2}"
MAX_CONFLICT_ROUNDS="${WAYPOINT_MAX_CONFLICT_ROUNDS:-2}"

mkdir -p "$WORKTREE_ROOT" "$STATE_DIR" "$LOG_DIR"

log() {
  printf '[%s] %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*" >> "$LOG_FILE"
}

acquire_lock() {
  if ! mkdir "$LOCK_DIR" 2>/dev/null; then
    log "Another orchestrator run holds the lock ($LOCK_DIR); exiting."
    exit 0
  fi
  trap 'rmdir "$LOCK_DIR" 2>/dev/null || true' EXIT
}

require_tools() {
  local t
  for t in git gh claude codex jq; do
    command -v "$t" >/dev/null 2>&1 || { log "Required tool '$t' not found on PATH; aborting."; exit 1; }
  done
}

ensure_authenticated_remote() {
  # cron runs in a different macOS security session than an interactive
  # login shell. The osxkeychain git credential helper depends on
  # session identity for some keychain items, not just environment
  # variables -- confirmed live: cron's git push/fetch failed with
  # "could not read Username ... Device not configured" (git falling
  # through to an interactive prompt with no TTY to prompt on) even
  # though PATH and Full Disk Access were both already fixed. Embedding
  # gh's token directly in this clone's remote URL sidesteps the
  # keychain/session dependency entirely -- it's a plain value in a
  # local, uncommitted .git/config, not looked up via the keychain at
  # request time. Re-applied every run (cheap, idempotent) so a rotated
  # token is always picked up.
  local https_url token host_and_path
  https_url="$(git -C "$REPO_ROOT" remote get-url origin)"
  token="$(gh auth token 2>/dev/null || true)"
  if [[ -n "$token" ]]; then
    host_and_path="${https_url#https://}"
    git -C "$CONTROL_DIR" remote set-url origin "https://x-access-token:${token}@${host_and_path}"
  else
    log "ensure_authenticated_remote: 'gh auth token' returned nothing; leaving existing remote URL as-is."
  fi
}

sync_control_clone() {
  if [[ ! -d "$CONTROL_DIR/.git" ]]; then
    local origin_url
    origin_url="$(git -C "$REPO_ROOT" remote get-url origin)"
    log "Cloning control clone from $origin_url into $CONTROL_DIR"
    git clone --quiet "$origin_url" "$CONTROL_DIR"
  fi
  ensure_authenticated_remote
  git -C "$CONTROL_DIR" fetch origin main --quiet
  git -C "$CONTROL_DIR" checkout main --quiet
  git -C "$CONTROL_DIR" reset --hard origin/main --quiet
  REPO_SLUG="$(git -C "$CONTROL_DIR" remote get-url origin | sed -E 's#.*[:/]([^/]+/[^/]+)$#\1#; s/\.git$//')"
}

push_control_main() {
  local msg="$1"
  git -C "$CONTROL_DIR" add -A
  if git -C "$CONTROL_DIR" diff --cached --quiet; then
    return 0
  fi
  git -C "$CONTROL_DIR" commit --quiet -m "$msg"
  if ! git -C "$CONTROL_DIR" push --quiet origin main; then
    if ! git -C "$CONTROL_DIR" pull --rebase --quiet origin main || ! git -C "$CONTROL_DIR" push --quiet origin main; then
      # A genuine conflict, not just a fast-forward gap. Bail out to a clean
      # state rather than leaving the control clone mid-rebase (which
      # previously crashed the whole script and blocked every later PR in
      # the same run from being processed at all).
      git -C "$CONTROL_DIR" rebase --abort 2>/dev/null || true
      log "push_control_main: conflict pushing '$msg'; left un-pushed, needs manual resolution in $CONTROL_DIR."
      return 1
    fi
  fi
}

# ---- task file frontmatter helpers -----------------------------------------

frontmatter_field() {
  local file="$1" field="$2"
  awk -v f="$field" '
    /^---$/ { c++; next }
    c==1 && $0 ~ "^"f":" { sub("^"f":[ ]*",""); print; exit }
  ' "$file"
}

set_frontmatter_field() {
  local file="$1" field="$2" value="$3"
  if grep -q "^${field}:" "$file"; then
    sed -i '' "s|^${field}:.*|${field}: ${value}|" "$file"
    return 0
  fi
  # The field has no existing line in this file's frontmatter yet (e.g. an
  # older task file authored before this field existed, or a QUEUED file
  # Codex wrote from a stale template). sed above only ever rewrites an
  # existing line, so silently doing nothing here would make every later
  # increment of this field invisible to frontmatter_field() forever --
  # comparisons like `(( conflict_rounds >= MAX_CONFLICT_ROUNDS ))` would
  # always see 0 and never stall. Insert it just above the closing `---` of
  # the frontmatter block instead.
  awk -v f="$field" -v v="$value" '
    BEGIN { in_fm=0; inserted=0 }
    /^---$/ {
      in_fm++
      if (in_fm==2 && !inserted) { print f": "v; inserted=1 }
      print
      next
    }
    { print }
  ' "$file" > "${file}.tmp" && mv "${file}.tmp" "$file"
}

task_files() {
  find "$CONTROL_DIR/agent/tasks" -maxdepth 1 -name '*.md' ! -name 'README.md' 2>/dev/null | sort
}

find_task_file_for_branch() {
  local branch="$1" f
  for f in $(task_files); do
    [[ "$(frontmatter_field "$f" branch)" == "$branch" ]] && { echo "$f"; return 0; }
  done
  return 1
}

# ---- prompt rendering -------------------------------------------------------

render_worker_prompt() {
  local task_file_rel="$1" branch="$2" fix_note="$3"
  awk '/^---$/{f=1; next} f' "$AUTOMATION_DIR/worker-prompt.md" \
    | sed -e "s|{{TASK_FILE}}|${task_file_rel}|g" \
          -e "s|{{BRANCH}}|${branch}|g" \
          -e "s|{{FIX_ROUND_NOTE}}|${fix_note}|g"
}

render_review_prompt() {
  local pr_number="$1" branch="$2" task_file_rel="$3" feature_slug="$4"
  awk '/^---$/{f=1; next} f' "$AUTOMATION_DIR/review-prompt.md" \
    | sed -e "s|{{PR_NUMBER}}|${pr_number}|g" \
          -e "s|{{BRANCH}}|${branch}|g" \
          -e "s|{{TASK_FILE}}|${task_file_rel}|g" \
          -e "s|{{FEATURE_SLUG}}|${feature_slug}|g"
}

# ---- dispatch phase ---------------------------------------------------------

count_in_progress() {
  local f n=0
  for f in $(task_files); do
    [[ "$(frontmatter_field "$f" status)" == "IN_PROGRESS" ]] && n=$((n+1))
  done
  echo "$n"
}

dispatch_worker() {
  local task_file="$1" branch="$2" wt_dir="$3" fix_note="$4" name="$5"
  local task_file_rel="agent/tasks/$(basename "$task_file")"
  local prompt
  prompt="$(render_worker_prompt "$task_file_rel" "$branch" "$fix_note")"
  local raw
  # --bg conflicts with -p/--print (an unattachable job would result); the
  # prompt is positional here instead. Confirmed empirically against the
  # installed claude CLI, which rejects the -p form outright.
  raw="$(cd "$wt_dir" && claude --bg "$prompt" --permission-mode bypassPermissions --name "$name" 2>>"$LOG_FILE")"
  log "claude --bg raw output for $name: $raw"
  # Confirmed format: first line is "backgrounded · <id> · <name>", followed
  # by a few "claude <subcommand> <id> ..." help lines. Take field 2 of line
  # 1, not the last line (which is one of those help lines, not the id).
  echo "$raw" | head -1 | awk -F' · ' '{print $2}' | tr -d '[:space:]'
}

dispatch_phase() {
  local slots=$(( MAX_PARALLEL - $(count_in_progress) ))
  if (( slots <= 0 )); then
    log "No free dispatch slots (MAX_PARALLEL=$MAX_PARALLEL)."
    return 0
  fi

  local f
  for f in $(task_files); do
    (( slots <= 0 )) && break
    [[ "$(frontmatter_field "$f" status)" == "QUEUED" ]] || continue

    local num slug branch wt_dir
    num="$(frontmatter_field "$f" task_number)"
    slug="$(frontmatter_field "$f" feature_slug)"
    branch="task/${num}-${slug}"
    wt_dir="$WORKTREE_ROOT/task-${num}-${slug}"

    set_frontmatter_field "$f" status IN_PROGRESS
    set_frontmatter_field "$f" branch "$branch"
    set_frontmatter_field "$f" worktree "$wt_dir"
    set_frontmatter_field "$f" claimed_at "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    push_control_main "Claim task ${num}-${slug} for automated dispatch"

    git -C "$CONTROL_DIR" worktree add "$wt_dir" -b "$branch" origin/main --quiet

    local session_id
    session_id="$(dispatch_worker "$f" "$branch" "$wt_dir" "" "task-${num}-${slug}")"
    set_frontmatter_field "$f" session "${session_id:-unknown}"
    push_control_main "Record worker session for task ${num}-${slug}"

    log "Dispatched task ${num}-${slug}: branch=$branch worktree=$wt_dir session=${session_id:-unknown}"
    slots=$((slots-1))
  done
}

# ---- review + merge phase ---------------------------------------------------

check_migration_collision() {
  # Only versions the branch actually *introduces* (added since it diverged
  # from main) matter here. Comparing the branch's full migration list
  # against main's always overlaps -- every branch inherits main's existing
  # migrations by definition -- so that comparison alone is not a collision
  # check at all; it was confirmed to false-positive on every single branch
  # during the first live run of this pipeline.
  local branch="$1" dir="backend/src/main/resources/db/migration"
  git -C "$CONTROL_DIR" fetch origin main "$branch" --quiet
  local merge_base new_files new_versions main_files v branch_file main_file
  merge_base="$(git -C "$CONTROL_DIR" merge-base origin/main "origin/$branch" 2>/dev/null || true)"
  [[ -z "$merge_base" ]] && return 0

  new_files="$(git -C "$CONTROL_DIR" diff --diff-filter=A --name-only "$merge_base" "origin/$branch" -- "$dir" 2>/dev/null \
    | xargs -n1 basename 2>/dev/null || true)"
  new_versions="$(echo "$new_files" | grep -oE '^V[0-9]+' | sort -u || true)"
  [[ -z "$new_versions" ]] && return 0

  # A within-branch duplicate (two new files claiming the same version) is
  # always a real collision.
  [[ -n "$(echo "$new_files" | grep -oE '^V[0-9]+' | sort | uniq -d)" ]] && return 1

  main_files="$(git -C "$CONTROL_DIR" ls-tree -r --name-only "origin/main" -- "$dir" 2>/dev/null \
    | xargs -n1 basename 2>/dev/null || true)"
  for v in $new_versions; do
    main_file="$(echo "$main_files" | grep "^${v}__" || true)"
    [[ -z "$main_file" ]] && continue
    branch_file="$(echo "$new_files" | grep "^${v}__" || true)"
    [[ "$branch_file" != "$main_file" ]] && return 1
  done
  return 0
}

# Pure verdict-parsing logic, kept separate from the actual `codex exec`
# invocation so it can be unit-tested against canned exit codes and message
# files without shelling out to Codex. `codex_status` is codex exec's own
# exit code (0 on a normal run; nonzero means the invocation itself failed
# -- a crash, auth failure, timeout, etc. -- not a review outcome).
#
# REVIEW_ERROR means review infrastructure failed, not that Codex reviewed
# the PR and found nothing wrong. It must never be treated as ACCEPTED
# (that would authorize a merge nobody actually reviewed) or as BLOCKING
# (that would burn a fix_rounds attempt, and dispatch a worker to "fix"
# findings that were never actually recorded).
determine_review_verdict() {
  local pr_number="$1" codex_status="$2" msg_file="$3"

  if (( codex_status != 0 )); then
    log "PR #$pr_number: codex exec exited $codex_status; treating as REVIEW_ERROR (will retry, not BLOCKING)."
    echo REVIEW_ERROR
    return 0
  fi

  if [[ ! -s "$msg_file" ]]; then
    log "PR #$pr_number: codex exec produced no output message; treating as REVIEW_ERROR (will retry, not BLOCKING)."
    echo REVIEW_ERROR
    return 0
  fi

  if grep -q '^REVIEW_VERDICT: ACCEPTED$' "$msg_file" 2>/dev/null; then
    echo ACCEPTED
  elif grep -q '^REVIEW_VERDICT: BLOCKING$' "$msg_file" 2>/dev/null; then
    echo BLOCKING
  else
    log "PR #$pr_number: no parseable REVIEW_VERDICT line from Codex; treating as REVIEW_ERROR (will retry, not BLOCKING)."
    echo REVIEW_ERROR
  fi
}

run_review() {
  local pr_number="$1" branch="$2" task_file="$3"
  local feature_slug task_file_rel prompt msg_file verdict codex_status
  feature_slug="$(frontmatter_field "$task_file" feature_slug)"
  task_file_rel="agent/tasks/$(basename "$task_file")"
  prompt="$(render_review_prompt "$pr_number" "$branch" "$task_file_rel" "$feature_slug")"
  msg_file="$(mktemp "$STATE_DIR/codex-msg.XXXXXX")"

  git -C "$CONTROL_DIR" fetch origin "$branch" --quiet
  git -C "$CONTROL_DIR" checkout -B "review-${pr_number}" "origin/${branch}" --quiet

  codex_status=0
  (cd "$CONTROL_DIR" && codex exec --dangerously-bypass-approvals-and-sandbox \
    --output-last-message "$msg_file" "$prompt") >> "$LOG_FILE" 2>&1 || codex_status=$?

  # review-prompt.md already tells Codex to push its own findings commit;
  # only push here as a safety net, and only if there's actually something
  # local that isn't on origin yet (an unconditional push here previously
  # always logged a confusing non-fast-forward rejection, since Codex's own
  # push already landed the same commit).
  local origin_sha local_sha
  origin_sha="$(git -C "$CONTROL_DIR" rev-parse "origin/${branch}" 2>/dev/null || true)"
  local_sha="$(git -C "$CONTROL_DIR" rev-parse HEAD 2>/dev/null || true)"
  if [[ -n "$local_sha" && "$origin_sha" != "$local_sha" ]]; then
    git -C "$CONTROL_DIR" push --quiet origin "HEAD:$branch" 2>>"$LOG_FILE" \
      || log "PR #$pr_number: could not push Codex's review commit as a safety net (already pushed by Codex, or a real conflict)."
  fi
  git -C "$CONTROL_DIR" checkout main --quiet

  verdict="$(determine_review_verdict "$pr_number" "$codex_status" "$msg_file")"
  rm -f "$msg_file"
  echo "$verdict"
}

try_merge() {
  local pr_number="$1" branch="$2" task_file="$3"
  local check_state
  check_state="$(gh pr checks "$pr_number" --repo "$REPO_SLUG" --json name,state \
    --jq '(.[] | select(.name=="verify") | .state) // "MISSING"' 2>/dev/null || echo MISSING)"
  if [[ "$check_state" != "SUCCESS" ]]; then
    log "PR #$pr_number: required 'verify' check is '$check_state', not merging yet."
    return 0
  fi

  # A sibling task can merge in between this PR opening and this check --
  # both edited the same shared file (README's status prose,
  # agent/implementation-log.md's shared append point, or a genuinely
  # overlapping code file) and this branch is no longer cleanly mergeable.
  # Confirmed against two real parallel tasks. Self-heal with a bounded
  # rebase-and-resolve round rather than repeatedly failing gh pr merge.
  local mergeable_state
  mergeable_state="$(gh pr view "$pr_number" --repo "$REPO_SLUG" --json mergeable --jq '.mergeable' 2>/dev/null || echo UNKNOWN)"
  if [[ "$mergeable_state" == "CONFLICTING" ]]; then
    handle_conflict "$pr_number" "$branch" "$task_file"
    return 0
  elif [[ "$mergeable_state" != "MERGEABLE" ]]; then
    log "PR #$pr_number: mergeable state is '$mergeable_state' (GitHub may not have computed it yet), not merging yet."
    return 0
  fi

  if ! check_migration_collision "$branch"; then
    set_frontmatter_field "$task_file" status STALLED
    push_control_main "Task $branch stalled: Flyway migration version collision against main"
    log "PR #$pr_number ($branch): migration version collision detected; marked STALLED instead of merging."
    return 0
  fi

  if gh pr merge "$pr_number" --repo "$REPO_SLUG" --squash --delete-branch; then
    # gh pr merge's squash commit brings the PR branch's own last edit to
    # $task_file (e.g. status: IN_REVIEW, set by the worker) into
    # origin/main via GitHub's API -- a purely remote change this clone
    # doesn't know about yet. Editing $task_file locally without syncing
    # first diverges from that squash commit and fails to push as a
    # conflicting rebase (confirmed against a real merge).
    git -C "$CONTROL_DIR" fetch origin main --quiet
    git -C "$CONTROL_DIR" reset --hard origin/main --quiet
    set_frontmatter_field "$task_file" status MERGED
    set_frontmatter_field "$task_file" pr "$pr_number"
    push_control_main "Task $branch merged automatically via PR #$pr_number"
    local wt_dir
    wt_dir="$(frontmatter_field "$task_file" worktree)"
    [[ -n "$wt_dir" && -d "$wt_dir" ]] && git -C "$CONTROL_DIR" worktree remove "$wt_dir" --force || true
    log "PR #$pr_number ($branch): merged and worktree cleaned up."
  else
    log "PR #$pr_number ($branch): gh pr merge failed; will retry next run."
  fi
}

handle_conflict() {
  local pr_number="$1" branch="$2" task_file="$3"
  local conflict_rounds
  conflict_rounds="$(frontmatter_field "$task_file" conflict_rounds)"
  [[ -z "$conflict_rounds" ]] && conflict_rounds=0

  if (( conflict_rounds >= MAX_CONFLICT_ROUNDS )); then
    set_frontmatter_field "$task_file" status STALLED
    push_control_main "Task $branch stalled: merge conflict against main, exhausted $MAX_CONFLICT_ROUNDS automatic rebase attempts"
    log "PR #$pr_number ($branch): merge conflict persists after $MAX_CONFLICT_ROUNDS attempts; marked STALLED for a human."
    return 0
  fi

  conflict_rounds=$((conflict_rounds+1))
  local wt_dir
  wt_dir="$(frontmatter_field "$task_file" worktree)"
  if [[ -z "$wt_dir" || ! -d "$wt_dir" ]]; then
    wt_dir="$WORKTREE_ROOT/$(echo "$branch" | tr '/' '-')"
    git -C "$CONTROL_DIR" fetch origin "$branch" --quiet
    git -C "$CONTROL_DIR" worktree add "$wt_dir" "$branch" --quiet
  else
    # Always sync to the true remote tip before handing this worktree to a
    # worker, even if it already exists -- a worktree left over from an
    # earlier dispatch can be arbitrarily behind origin (e.g. Codex's own
    # review commit landed after the worktree was created), and resolving
    # conflicts from a stale base produces a merge that silently drops
    # commits instead of a real conflict. Confirmed the hard way: an
    # earlier manual fix on this exact class of bug had to be redone after
    # being caught doing precisely this.
    git -C "$wt_dir" fetch origin "$branch" --quiet
    git -C "$wt_dir" reset --hard "origin/$branch" --quiet
  fi

  set_frontmatter_field "$task_file" status IN_PROGRESS
  set_frontmatter_field "$task_file" worktree "$wt_dir"
  set_frontmatter_field "$task_file" conflict_rounds "$conflict_rounds"
  push_control_main "Rebase round $conflict_rounds for $branch (merge conflict against main)"

  local fix_note="This branch's PR now conflicts with main -- almost certainly because a sibling task merged in the meantime and touched the same shared file (commonly README.md's Status/endpoint-docs prose or agent/implementation-log.md's shared append point). Run: git fetch origin main && git merge origin/main. Resolve every conflict by KEEPING BOTH sides' additions where they are independent content -- two different doc sections, two different implementation-log entries, two different exception handlers or endpoints -- never drop the other task's work to make yours look cleaner. Only if the conflict is a genuine logical clash in application code (not docs/log prose, and not two independent additions like separate methods) should you use judgment about which version is correct, and record that decision explicitly in agent/implementation-log.md. Do not resolve a conflict in a Flyway migration's version number yourself -- if the conflict involves that, stop and set this task file's status to STALLED with an explanation instead, since that needs a human decision. Once resolved, re-run ./verify.sh to confirm the merge actually builds, commit the merge, and push."
  local session_id
  session_id="$(dispatch_worker "$task_file" "$branch" "$wt_dir" "$fix_note" "task-rebase${conflict_rounds}-$(echo "$branch" | tr '/' '-')")"
  set_frontmatter_field "$task_file" session "${session_id:-unknown}"
  push_control_main "Record rebase-round worker session for $branch"

  log "PR #$pr_number ($branch): dispatched rebase round $conflict_rounds to resolve merge conflict (session ${session_id:-unknown})."
}

handle_blocking() {
  local pr_number="$1" branch="$2" task_file="$3"
  local fix_rounds
  fix_rounds="$(frontmatter_field "$task_file" fix_rounds)"
  [[ -z "$fix_rounds" ]] && fix_rounds=0

  if (( fix_rounds >= MAX_FIX_ROUNDS )); then
    set_frontmatter_field "$task_file" status STALLED
    push_control_main "Task $branch stalled: exhausted $MAX_FIX_ROUNDS automatic fix rounds"
    log "PR #$pr_number ($branch): exhausted fix rounds; marked STALLED for a human."
    return 0
  fi

  fix_rounds=$((fix_rounds+1))
  local wt_dir
  wt_dir="$(frontmatter_field "$task_file" worktree)"
  if [[ -z "$wt_dir" || ! -d "$wt_dir" ]]; then
    wt_dir="$WORKTREE_ROOT/$(echo "$branch" | tr '/' '-')"
    git -C "$CONTROL_DIR" fetch origin "$branch" --quiet
    git -C "$CONTROL_DIR" worktree add "$wt_dir" "$branch" --quiet
  else
    git -C "$wt_dir" fetch origin "$branch" --quiet
    git -C "$wt_dir" reset --hard "origin/$branch" --quiet
  fi

  set_frontmatter_field "$task_file" status IN_PROGRESS
  set_frontmatter_field "$task_file" worktree "$wt_dir"
  set_frontmatter_field "$task_file" fix_rounds "$fix_rounds"
  push_control_main "Fix round $fix_rounds for $branch (Codex found BLOCKING findings)"

  local fix_note="This is automatic fix round ${fix_rounds} of ${MAX_FIX_ROUNDS}. Codex's review findings are already appended to the linked product brief with ACCEPTED/REJECTED/DEFERRED verdicts. Read them, apply every ACCEPTED finding, re-run ./verify.sh, and push."
  local session_id
  session_id="$(dispatch_worker "$task_file" "$branch" "$wt_dir" "$fix_note" "task-fix${fix_rounds}-$(echo "$branch" | tr '/' '-')")"
  set_frontmatter_field "$task_file" session "${session_id:-unknown}"
  push_control_main "Record fix-round worker session for $branch"

  log "PR #$pr_number ($branch): dispatched fix round $fix_rounds (session ${session_id:-unknown})."
}

review_and_merge_phase() {
  local prs
  prs="$(gh pr list --repo "$REPO_SLUG" --state open --base main --json number,headRefName \
    --jq '.[] | select(.headRefName | startswith("task/")) | "\(.number)\t\(.headRefName)"' 2>/dev/null || true)"
  [[ -z "$prs" ]] && { log "No open task/* PRs."; return 0; }

  # Read the PR list from fd 3, not stdin (fd 0): codex exec, called deep
  # inside this loop's body, is an interactive-capable CLI that can read
  # from/seek on whatever it inherits as stdin. Sharing fd 0 with the
  # loop's own `read` let it silently consume the remaining lines of $prs
  # after the first PR was processed -- the second PR in the list was
  # never even attempted, with no error and no log line, because the
  # loop's next `read` just saw EOF. Confirmed against a real two-PR run.
  while IFS=$'\t' read -r -u 3 pr_number branch; do
    [[ -z "$pr_number" ]] && continue
    local task_file
    if ! task_file="$(find_task_file_for_branch "$branch")"; then
      log "PR #$pr_number ($branch): no matching task file; skipping."
      continue
    fi

    local head_sha state_file last_sha verdict
    head_sha="$(gh pr view "$pr_number" --repo "$REPO_SLUG" --json headRefOid --jq .headRefOid)"
    state_file="$STATE_DIR/pr-${pr_number}.json"
    last_sha=""
    verdict=""
    if [[ -f "$state_file" ]]; then
      last_sha="$(jq -r '.last_reviewed_sha // empty' "$state_file")"
      verdict="$(jq -r '.verdict // empty' "$state_file")"
    fi

    if [[ "$head_sha" != "$last_sha" ]]; then
      verdict="$(run_review "$pr_number" "$branch" "$task_file")"
      if [[ "$verdict" == "REVIEW_ERROR" ]]; then
        # Review infrastructure failed (codex exec crashed, produced no
        # output, or a malformed/missing verdict line) rather than
        # completing a real review. Do not persist last_reviewed_sha: with
        # no state written, the next run's head_sha still won't match
        # last_sha, so this PR is retried from scratch next tick instead of
        # being silently treated as reviewed. Deliberately does not fall
        # through to the case statement below -- no fix round, no
        # fix_rounds increment, no merge.
        log "PR #$pr_number: review infrastructure failure; leaving PR for another review attempt next run."
        continue
      fi
      # Codex's own review commit moves the PR's head; record *that* sha,
      # not the pre-review one, or the next tick sees a "new" commit (the
      # review itself) and re-reviews it every single run.
      head_sha="$(gh pr view "$pr_number" --repo "$REPO_SLUG" --json headRefOid --jq .headRefOid)"
      printf '{"last_reviewed_sha":"%s","verdict":"%s"}\n' "$head_sha" "$verdict" > "$state_file"
    fi

    case "$verdict" in
      ACCEPTED) try_merge "$pr_number" "$branch" "$task_file" ;;
      BLOCKING) handle_blocking "$pr_number" "$branch" "$task_file" ;;
      *) log "PR #$pr_number: no usable verdict yet." ;;
    esac
  done 3<<< "$prs"
}

main() {
  require_tools
  acquire_lock
  sync_control_clone
  review_and_merge_phase
  dispatch_phase
}

# Allow agent/automation/tests/orchestrator_test.sh to `source` this file to
# exercise individual functions (prompt rendering, frontmatter helpers,
# verdict parsing, migration-collision detection) without running a real
# unattended pass against GitHub/Codex/Claude. Only run main() when this
# file is executed directly, exactly like the historical `if __name__ ==
# "__main__"` pattern.
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  main "$@"
fi
