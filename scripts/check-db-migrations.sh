#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
REPO_ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel)"
MIGRATION_DIR="$REPO_ROOT/backend/projectmentor-server/src/main/resources/db/migration"
EXPECTED_V1="V1__baseline_schema.sql"

fail() {
  printf 'FAIL database migration hygiene: %s\n' "$1" >&2
  exit 1
}

if [[ ! -d "$MIGRATION_DIR" || -L "$MIGRATION_DIR" ]]; then
  fail 'migration directory is missing or is not a real directory'
fi

if [[ ! -f "$MIGRATION_DIR/$EXPECTED_V1" || -L "$MIGRATION_DIR/$EXPECTED_V1" ]]; then
  fail "required migration is missing or is not a regular file: $EXPECTED_V1"
fi

declare -A SEEN_VERSIONS=()
declare -a VALID_MIGRATIONS=()
shopt -s extglob

ENTRIES_FILE="$(mktemp)"
cleanup() {
  rm -f -- "$ENTRIES_FILE"
}
trap cleanup EXIT

find "$MIGRATION_DIR" -mindepth 1 -maxdepth 1 -print0 | sort -z >"$ENTRIES_FILE"

while IFS= read -r -d '' entry; do
  entry_name="${entry##*/}"
  relative_path="${entry#"$REPO_ROOT"/}"

  if [[ -L "$entry" || ! -f "$entry" ]]; then
    fail "only regular migration files and an optional README.md are allowed: $relative_path"
  fi

  if ! git -C "$REPO_ROOT" ls-files --error-unmatch -- "$relative_path" >/dev/null 2>&1; then
    fail "migration directory entry is not tracked by Git: $relative_path"
  fi

  if [[ "$entry_name" == 'README.md' ]]; then
    continue
  fi

  if [[ ! "$entry_name" =~ ^V([0-9]+)__([a-z][a-z0-9]*(_[a-z0-9]+)*)\.sql$ ]]; then
    fail "invalid migration filename: $relative_path"
  fi

  raw_version="${BASH_REMATCH[1]}"
  normalized_version="${raw_version##+(0)}"
  if [[ -z "$normalized_version" ]]; then
    fail "migration version must be a positive integer: $relative_path"
  fi

  if [[ -n "${SEEN_VERSIONS[$normalized_version]+present}" ]]; then
    fail "duplicate numeric version V$normalized_version: ${SEEN_VERSIONS[$normalized_version]} and $relative_path"
  fi
  SEEN_VERSIONS["$normalized_version"]="$relative_path"

  if [[ ! -s "$entry" ]]; then
    fail "migration file is empty: $relative_path"
  fi

  VALID_MIGRATIONS+=("V${normalized_version}"$'\t'"$relative_path")
done <"$ENTRIES_FILE"

if [[ -z "${SEEN_VERSIONS[1]+present}" || "${SEEN_VERSIONS[1]}" != "backend/projectmentor-server/src/main/resources/db/migration/$EXPECTED_V1" ]]; then
  fail "numeric version V1 must be exactly $EXPECTED_V1"
fi

if ((${#VALID_MIGRATIONS[@]} == 0)); then
  fail 'no SQL migration files found'
fi

printf '%s\n' "${VALID_MIGRATIONS[@]}"
printf 'PASS database migration hygiene\n'
