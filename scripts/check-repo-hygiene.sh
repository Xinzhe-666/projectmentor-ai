#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel)"
cd "$REPO_ROOT"

FAILED=0

while IFS= read -r -d '' path; do
  case "$path" in
    .env.example|frontend/projectmentor-web/.env.development|frontend/projectmentor-web/.env.production)
      continue
      ;;
  esac

  normalized_path="${path,,}"
  basename="${normalized_path##*/}"
  forbidden=0

  case "$normalized_path" in
    target/*|*/target/*|dist/*|*/dist/*|node_modules/*|*/node_modules/*|.idea/*|*/.idea/*|.vscode/*|*/.vscode/*)
      forbidden=1
      ;;
  esac

  case "$basename" in
    .env|.env.*|*.env|*.class|*.jar|dist.zip|frontend-dist.zip|*.key|*.pem|.ds_store|thumbs.db)
      forbidden=1
      ;;
  esac

  if (( forbidden != 0 )); then
    printf 'FAIL tracked sensitive/build artifact: %s\n' "$path" >&2
    FAILED=1
  fi
done < <(git ls-files -z)

if (( FAILED != 0 )); then
  exit 1
fi

printf 'PASS repository hygiene\n'
