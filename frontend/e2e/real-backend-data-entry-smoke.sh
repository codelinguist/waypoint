#!/usr/bin/env bash
# Real-backend smoke flow for household data entry (WAP-25).
#
# Same isolated, disposable stack as e2e/real-backend-smoke.sh (see that
# script's header for the docker-compose.test.yml rationale) — an anonymous
# Postgres volume, separate host ports, never touching the shared household
# database volume. Run one at a time (not concurrently with
# real-backend-smoke.sh): both use the same default isolated ports.
#
# Unlike real-backend-smoke.sh, this seeds a household with NO records at
# all — every asset, liability, income stream, obligation, goal, snapshot,
# and planning assumption is created through the real UI by
# data-entry-smoke.spec.ts (real Chromium), which then confirms each one
# renders on its read-only view and, for the asset correction, that the
# prior value is preserved in the real valuation-history endpoint (D021/D022).
# Screenshots are saved under e2e-evidence/ and copied into this ticket's
# durable evidence folder. Tears the isolated stack down afterward (`down
# -v`), discarding the anonymous volume.
#
# Requires: docker, curl, jq, and the frontend's npm dependencies/Playwright
# browsers installed (see README.md "Frontend E2E tests"). Run from the
# repository root.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_ROOT"

PROJECT="waypoint-e2e-data-entry-smoke-$$"
DB_PORT="${TEST_DB_PORT:-15432}"
SERVER_PORT="${TEST_SERVER_PORT:-18080}"
FRONTEND_PORT="${TEST_FRONTEND_PORT:-15173}"
COMPOSE="docker compose -p $PROJECT -f docker-compose.yml -f docker-compose.test.yml"

export TEST_DB_PORT="$DB_PORT" TEST_SERVER_PORT="$SERVER_PORT" TEST_FRONTEND_PORT="$FRONTEND_PORT"
export DB_PORT SERVER_PORT FRONTEND_PORT

cleanup() {
  echo "--- tearing down isolated stack ($PROJECT) ---"
  $COMPOSE down -v
}
trap cleanup EXIT

echo "--- building images ---"
$COMPOSE build postgres app frontend

echo "--- starting database and backend ---"
$COMPOSE up -d postgres app

BACKEND="http://localhost:${SERVER_PORT}"
FRONTEND="http://localhost:${FRONTEND_PORT}"

echo "--- waiting for backend health ---"
for _ in $(seq 1 60); do
  if curl -fsS "$BACKEND/actuator/health" >/dev/null 2>&1; then
    break
  fi
  sleep 2
done
curl -fsS "$BACKEND/actuator/health" | grep -q '"status":"UP"'

echo "--- seeding an empty household (no records — this flow creates all of them through the UI) ---"
HOUSEHOLD_NAME="E2E Data Entry Smoke Household"
HOUSEHOLD_ID=$(curl -fsS -X POST "$BACKEND/api/households" \
  -H 'Content-Type: application/json' \
  -d "{\"name\": \"$HOUSEHOLD_NAME\", \"baseCurrency\": \"PHP\"}" | jq -r '.id')
echo "household id: $HOUSEHOLD_ID"

echo "--- starting the frontend configured for the synthetic household ---"
HOUSEHOLD_ID="$HOUSEHOLD_ID" $COMPOSE up -d frontend

echo "--- waiting for the frontend to be served ---"
for _ in $(seq 1 60); do
  if curl -fsS "$FRONTEND/" >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

echo "--- exercising every create/correct flow against the real React app (real Chromium) ---"
mkdir -p frontend/e2e-evidence
(
  cd frontend
  SMOKE_BASE_URL="$FRONTEND" SMOKE_HOUSEHOLD_ID="$HOUSEHOLD_ID" SMOKE_HOUSEHOLD_NAME="$HOUSEHOLD_NAME" \
    npx playwright test --config=playwright.data-entry-smoke.config.ts
)

EVIDENCE_DIR="agent/ui/household-data-entry/evidence/implementation"
mkdir -p "$EVIDENCE_DIR"
cp frontend/e2e-evidence/*.png "$EVIDENCE_DIR/" 2>/dev/null || true

echo "PASS: every WAP-25 create/correct flow reconciled through the real API and frontend proxy."
