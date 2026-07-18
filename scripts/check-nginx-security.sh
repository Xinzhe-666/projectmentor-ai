#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${1:-https://projectmentorai.com}"
BASE_URL="${BASE_URL%/}"

case "$BASE_URL" in
  http://*|https://*) ;;
  *)
    printf 'FAIL Base URL must start with http:// or https://\n' >&2
    exit 2
    ;;
esac

if [[ "$BASE_URL" =~ ^https?://[^/]*@ ]]; then
  printf 'FAIL Base URL must not contain credentials\n' >&2
  exit 2
fi

if ! command -v curl >/dev/null 2>&1; then
  printf 'FAIL curl is required\n' >&2
  exit 2
fi

PASS_COUNT=0
FAIL_COUNT=0
TEMP_DIR="$(mktemp -d)"
BODY_FILE="$TEMP_DIR/response-body"
trap 'rm -rf "$TEMP_DIR"' EXIT

pass() {
  PASS_COUNT=$((PASS_COUNT + 1))
  printf 'PASS %-32s %s\n' "$1" "$2"
}

fail() {
  FAIL_COUNT=$((FAIL_COUNT + 1))
  printf 'FAIL %-32s %s\n' "$1" "$2"
}

fetch() {
  local path="$1"
  : >"$BODY_FILE"
  HTTP_STATUS=""

  if HTTP_STATUS="$(curl \
    --silent \
    --show-error \
    --location \
    --max-redirs 5 \
    --path-as-is \
    --connect-timeout 5 \
    --max-time 15 \
    --output "$BODY_FILE" \
    --write-out '%{http_code}' \
    --user-agent 'ProjectMentor-Nginx-Security-Check/1.0' \
    "${BASE_URL}${path}")"; then
    return 0
  fi

  HTTP_STATUS="curl-error"
  return 1
}

looks_like_spa() {
  grep -Eiq "ProjectMentor AI|<!DOCTYPE html>|<div[^>]+id=['\"]app['\"]|/assets/index-[^'\"]+\.(js|css)" "$BODY_FILE"
}

check_normal() {
  local path="$1"

  if ! fetch "$path"; then
    fail "$path" "request failed"
    return
  fi

  if [[ "$HTTP_STATUS" == "200" ]]; then
    pass "$path" "HTTP 200"
  else
    fail "$path" "expected HTTP 200, got $HTTP_STATUS"
  fi
}

check_sensitive() {
  local path="$1"

  if ! fetch "$path"; then
    fail "$path" "request failed"
    return
  fi

  if [[ "$HTTP_STATUS" == "403" || "$HTTP_STATUS" == "404" ]]; then
    pass "$path" "HTTP $HTTP_STATUS"
  elif [[ "$HTTP_STATUS" == "200" ]] && looks_like_spa; then
    fail "$path" "HTTP 200 with Vue SPA/index.html content"
  elif [[ "$HTTP_STATUS" == "200" ]]; then
    fail "$path" "sensitive path returned HTTP 200"
  else
    fail "$path" "expected HTTP 403 or 404, got $HTTP_STATUS"
  fi
}

check_scan() {
  local path="$1"

  if ! fetch "$path"; then
    fail "$path" "request failed"
    return
  fi

  if [[ "$HTTP_STATUS" == "403" ]]; then
    pass "$path" "HTTP 403"
  else
    fail "$path" "expected HTTP 403, got $HTTP_STATUS"
  fi
}

printf 'ProjectMentor AI Nginx security regression check\n'
printf 'Target: %s\n\n' "$BASE_URL"

NORMAL_PATHS=(
  "/"
  "/login"
  "/register"
)

SENSITIVE_PATHS=(
  "/.env"
  "/.env.production"
  "/backup.sql"
  "/database.dump"
  "/source.zip"
  "/@fs/etc/passwd"
  "/swagger-ui/index.html"
  "/swagger-ui.html"
  "/doc.html"
  "/v3/api-docs"
  "/actuator/env"
  "/graphql"
  "/api/install"
)

SCAN_PATHS=(
  "/shell"
  "/test?cmd=wget"
  "/test?cmd=chmod"
  "/test?cmd=rm+-rf"
)

for path in "${NORMAL_PATHS[@]}"; do
  check_normal "$path"
done

for path in "${SENSITIVE_PATHS[@]}"; do
  check_sensitive "$path"
done

for path in "${SCAN_PATHS[@]}"; do
  check_scan "$path"
done

printf '\nSummary: %d passed, %d failed\n' "$PASS_COUNT" "$FAIL_COUNT"

if ((FAIL_COUNT > 0)); then
  exit 1
fi
