#!/usr/bin/env bash
# Real-backend smoke flow for the financial-position dashboard (WAP-15).
#
# Brings up an isolated, disposable stack (postgres + backend + frontend)
# using docker-compose.test.yml — an anonymous Postgres volume, separate host
# ports — so this never touches the shared household database volume. Seeds
# one synthetic household with mixed-currency assets and liabilities through
# the real REST API, then reads it back through the real
# GET /api/households/{id}/financial-position endpoint (proxied by the real
# nginx frontend container, exactly as the browser would reach it) and
# asserts the totals reconcile. Tears the isolated stack down afterward
# (`down -v`), discarding the anonymous volume.
#
# Requires: docker, curl, jq. Run from the repository root.
set -euo pipefail

PROJECT="waypoint-e2e-smoke-$$"
DB_PORT="${TEST_DB_PORT:-15432}"
SERVER_PORT="${TEST_SERVER_PORT:-18080}"
FRONTEND_PORT="${TEST_FRONTEND_PORT:-15173}"
COMPOSE="docker compose -p $PROJECT -f docker-compose.yml -f docker-compose.test.yml"

cleanup() {
  echo "--- tearing down isolated stack ($PROJECT) ---"
  $COMPOSE down -v
}
trap cleanup EXIT

echo "--- building and starting isolated stack ---"
TEST_DB_PORT="$DB_PORT" TEST_SERVER_PORT="$SERVER_PORT" TEST_FRONTEND_PORT="$FRONTEND_PORT" \
  $COMPOSE up -d --build postgres app frontend

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

echo "--- seeding a synthetic household ---"
HOUSEHOLD_ID=$(curl -fsS -X POST "$BACKEND/api/households" \
  -H 'Content-Type: application/json' \
  -d '{"name": "E2E Smoke Household", "baseCurrency": "PHP"}' | jq -r '.id')
echo "household id: $HOUSEHOLD_ID"

curl -fsS -X POST "$BACKEND/api/households/$HOUSEHOLD_ID/assets" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Smoke Checking Account","assetType":"BANK_ACCOUNT","estimatedValue":"185000.00","planningValue":"185000.00","currency":"PHP","valuedAt":"2026-09-08","liquidity":"LIQUID"}' \
  >/dev/null

curl -fsS -X POST "$BACKEND/api/households/$HOUSEHOLD_ID/assets" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Smoke US Brokerage","assetType":"INVESTMENT","estimatedValue":"12500.50","planningValue":"12500.50","currency":"USD","valuedAt":"2026-09-01","liquidity":"LIQUID"}' \
  >/dev/null

curl -fsS -X POST "$BACKEND/api/households/$HOUSEHOLD_ID/liabilities" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Smoke Credit Card","liabilityType":"CREDIT_CARD","outstandingBalance":"8000.00","currency":"USD","balanceAsOf":"2026-09-02"}' \
  >/dev/null

echo "--- reading financial position through the real frontend proxy (nginx -> backend) ---"
RESPONSE=$(curl -fsS "$FRONTEND/api/households/$HOUSEHOLD_ID/financial-position")
echo "$RESPONSE" | jq .

PHP_NET=$(echo "$RESPONSE" | jq -r '.totalsByCurrency[] | select(.currency=="PHP") | .netWorth')
USD_NET=$(echo "$RESPONSE" | jq -r '.totalsByCurrency[] | select(.currency=="USD") | .netWorth')

[ "$PHP_NET" = "185000.00" ] || { echo "FAIL: expected PHP net worth 185000.00, got $PHP_NET"; exit 1; }
[ "$USD_NET" = "4500.50" ] || { echo "FAIL: expected USD net worth 4500.50, got $USD_NET"; exit 1; }

# Money fields must be JSON strings, never numbers (exact-decimal transport contract).
TYPE_CHECK=$(echo "$RESPONSE" | jq -r '.totalsByCurrency[0].netWorth | type')
[ "$TYPE_CHECK" = "string" ] || { echo "FAIL: netWorth must be a JSON string, got $TYPE_CHECK"; exit 1; }

echo "--- confirming the frontend document itself is served (same-origin, no CORS needed) ---"
curl -fsS "$FRONTEND/" | grep -qi '<div id="root">'

echo "PASS: real-backend smoke flow reconciled totals from source rows through the real API and frontend proxy."
