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
MAX_FIX_ROUNDS="${WAYPOINT_MAX_FIX_ROUNDS:-2}"

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

sync_control_clone() {
  if [[ ! -d "$CONTROL_DIR/.git" ]]; then
    local origin_url
    origin_url="$(git -C "$REPO_ROOT" remote get-url origin)"
    log "Cloning control clone from $origin_url into $CONTROL_DIR"
    git clone --quiet "$origin_url" "$CONTROL_DIR"
  fi
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
    git -C "$CONTROL_DIR" pull --rebase --quiet origin main
    git -C "$CONTROL_DIR" push --quiet origin main
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
  sed -i '' "s|^${field}:.*|${field}: ${value}|" "$file"
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
  raw="$(cd "$wt_dir" && claude -p "$prompt" --permission-mode bypassPermissions --bg --name "$name" 2>>"$LOG_FILE")"
  log "claude --bg raw output for $name: $raw"
  # claude --bg prints the short background-session id; take the last
  # non-empty line as a best effort. Verify with `claude agents` if this
  # ever looks wrong — see agent/automation/README.md.
  echo "$raw" | awk 'NF{line=$0} END{print line}'
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
  local branch="$1" dir="backend/src/main/resources/db/migration"
  git -C "$CONTROL_DIR" fetch origin main "$branch" --quiet
  local main_versions branch_versions dup collision
  main_versions="$(git -C "$CONTROL_DIR" ls-tree -r --name-only "origin/main" -- "$dir" 2>/dev/null \
    | xargs -n1 basename 2>/dev/null | grep -oE '^V[0-9]+' | sort -u || true)"
  branch_versions="$(git -C "$CONTROL_DIR" ls-tree -r --name-only "origin/$branch" -- "$dir" 2>/dev/null \
    | xargs -n1 basename 2>/dev/null | grep -oE '^V[0-9]+' | sort || true)"
  dup="$(echo "$branch_versions" | uniq -d)"
  [[ -n "$dup" ]] && return 1
  collision="$(comm -12 <(echo "$main_versions") <(echo "$branch_versions" | sort -u) 2>/dev/null || true)"
  [[ -n "$collision" ]] && return 1
  return 0
}

run_review() {
  local pr_number="$1" branch="$2" task_file="$3"
  local feature_slug task_file_rel prompt msg_file verdict
  feature_slug="$(frontmatter_field "$task_file" feature_slug)"
  task_file_rel="agent/tasks/$(basename "$task_file")"
  prompt="$(render_review_prompt "$pr_number" "$branch" "$task_file_rel" "$feature_slug")"
  msg_file="$(mktemp "$STATE_DIR/codex-msg.XXXXXX")"

  git -C "$CONTROL_DIR" fetch origin "$branch" --quiet
  git -C "$CONTROL_DIR" checkout -B "review-${pr_number}" "origin/${branch}" --quiet

  (cd "$CONTROL_DIR" && codex exec --dangerously-bypass-approvals-and-sandbox \
    --output-last-message "$msg_file" "$prompt") >> "$LOG_FILE" 2>&1 || true

  git -C "$CONTROL_DIR" push --quiet origin "HEAD:$branch" || true
  git -C "$CONTROL_DIR" checkout main --quiet

  if grep -q '^REVIEW_VERDICT: ACCEPTED$' "$msg_file" 2>/dev/null; then
    verdict=ACCEPTED
  elif grep -q '^REVIEW_VERDICT: BLOCKING$' "$msg_file" 2>/dev/null; then
    verdict=BLOCKING
  else
    log "PR #$pr_number: no parseable REVIEW_VERDICT line from Codex; treating as BLOCKING (fail safe)."
    verdict=BLOCKING
  fi
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

  if ! check_migration_collision "$branch"; then
    set_frontmatter_field "$task_file" status STALLED
    push_control_main "Task $branch stalled: Flyway migration version collision against main"
    log "PR #$pr_number ($branch): migration version collision detected; marked STALLED instead of merging."
    return 0
  fi

  if gh pr merge "$pr_number" --repo "$REPO_SLUG" --squash --delete-branch; then
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

  while IFS=$'\t' read -r pr_number branch; do
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
      printf '{"last_reviewed_sha":"%s","verdict":"%s"}\n' "$head_sha" "$verdict" > "$state_file"
    fi

    case "$verdict" in
      ACCEPTED) try_merge "$pr_number" "$branch" "$task_file" ;;
      BLOCKING) handle_blocking "$pr_number" "$branch" "$task_file" ;;
      *) log "PR #$pr_number: no usable verdict yet." ;;
    esac
  done <<< "$prs"
}

main() {
  require_tools
  acquire_lock
  sync_control_clone
  review_and_merge_phase
  dispatch_phase
}

main "$@"
