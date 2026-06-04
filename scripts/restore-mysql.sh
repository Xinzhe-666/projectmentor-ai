#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

CONTAINER_NAME="projectmentor-mysql"

log() {
  printf '[restore-mysql] %s\n' "$1"
}

if [ "$#" -ne 1 ]; then
  log "Usage: bash scripts/restore-mysql.sh backups/mysql/xxx.sql"
  exit 1
fi

SQL_FILE="$1"

if [ ! -f "$SQL_FILE" ]; then
  log "SQL file not found: $SQL_FILE"
  exit 1
fi

log "Project root: $PROJECT_ROOT"
log "SQL file: $SQL_FILE"
log "恢复操作会覆盖或影响当前数据库，请确认已备份当前数据。"
printf 'Type YES to continue: '
read -r CONFIRM

if [ "$CONFIRM" != "YES" ]; then
  log "Restore cancelled."
  exit 1
fi

log "Importing SQL into MySQL container: $CONTAINER_NAME"
if docker exec -i "$CONTAINER_NAME" sh -c ': "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD is missing in container}"; : "${MYSQL_DATABASE:?MYSQL_DATABASE is missing in container}"; mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' < "$SQL_FILE"; then
  log "Restore completed successfully."
else
  log "Restore failed."
  exit 1
fi
