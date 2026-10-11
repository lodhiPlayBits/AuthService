#!/usr/bin/env bash
# Post-deploy smoke test from OUTSIDE the VM (GitHub runner -> internet -> DNS -> nginx).
# Covers what the VM-side checks in deploy.sh cannot: DNS, security group, public TLS.
# Usage: smoke-test.sh https://authvolt.fun
set -uo pipefail

BASE=${1:?usage: smoke-test.sh <base-url>}
ATTEMPTS=${ATTEMPTS:-3}     # retry the whole suite to ride out a transient network blip
WAIT=${WAIT:-15}
C=(curl -sS --max-time 15)

run_checks() {
  local fails=0 code body js bundle
  pass() { echo "  PASS $1"; }
  fail() { echo "::error::smoke test failed: $1"; fails=$((fails + 1)); }
  expect() {   # expect <name> <expected> <actual>
    if [ "$3" = "$2" ]; then pass "$1 ($3)"; else fail "$1 (expected $2, got $3)"; fi
  }

  # 1. Gateway -> auth-service health, over public DNS + TLS
  body=$("${C[@]}" -w '\n%{http_code}' "$BASE/health" 2>&1 || true)
  code=${body##*$'\n'}
  expect "GET /health" 200 "$code"
  if [ "$code" = 200 ] && ! grep -q '"UP"' <<<"$body"; then fail "/health does not report UP"; fi

  # 2. Login with a non-existent user: gateway Lua -> auth-service -> DB -> error handler
  code=$("${C[@]}" -o /dev/null -w '%{http_code}' -X POST "$BASE/api/v1/auth/login" \
         -H 'Content-Type: application/json' \
         -d '{"identifier":"smoke-test@authvolt.invalid","password":"smoke-test-not-a-real-password"}' 2>/dev/null || true)
  expect "POST /api/v1/auth/login (bad credentials)" 401 "$code"

  # 3. Notification service routed, JWT filter answers
  code=$("${C[@]}" -o /dev/null -w '%{http_code}' "$BASE/api/v1/notifications/stream" 2>/dev/null || true)
  expect "GET /api/v1/notifications/stream (no token)" 401 "$code"

  # 4. Frontend served
  body=$("${C[@]}" "$BASE/client/" 2>/dev/null || true)
  code=$("${C[@]}" -o /dev/null -w '%{http_code}' "$BASE/client/" 2>/dev/null || true)
  expect "GET /client/" 200 "$code"

  # 5. Frontend bundle built with the production API URL
  js=$(grep -oE '/client/assets/[^"]+\.js' <<<"$body" | head -1 || true)
  bundle=""
  [ -n "$js" ] && bundle=$("${C[@]}" "$BASE$js" 2>/dev/null || true)
  if [ -z "$bundle" ]; then
    fail "could not download the JS bundle referenced by /client/"
  elif grep -q "localhost:8080" <<<"$bundle"; then
    fail "client bundle contains localhost:8080 (VITE_API_URL missing at build time)"
  else
    pass "client bundle uses the production API URL"
  fi

  return "$fails"
}

for i in $(seq 1 "$ATTEMPTS"); do
  echo "Smoke test attempt $i/$ATTEMPTS against $BASE"
  if run_checks; then
    echo "All smoke tests passed."
    exit 0
  fi
  [ "$i" -lt "$ATTEMPTS" ] && { echo "Retrying in ${WAIT}s..."; sleep "$WAIT"; }
done
echo "::error::Smoke tests failed after $ATTEMPTS attempts."
exit 1
