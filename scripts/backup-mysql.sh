#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

CONTAINER_NAME="projectmentor-mysql"
BACKUP_DIR="backups/mysql"
TIMESTAMP="$(date '+%Y%m%d_%H%M%S')"
BACKUP_FILE="${BACKUP_DIR}/pmai_mysql_${TIMESTAMP}.sql"
TMP_FILE="${BACKUP_FILE}.tmp"

log() {
  printf '[backup-mysql] %s\n' "$1"
}

cleanup() {
  if [ -f "$TMP_FILE" ]; then
    rm -f "$TMP_FILE"
  fi
}
trap cleanup EXIT

log "Project root: $PROJECT_ROOT"
log "Creating backup directory: $BACKUP_DIR"
mkdir -p "$BACKUP_DIR"

log "Dumping MySQL database from container: $CONTAINER_NAME"
if docker exec "$CONTAINER_NAME" sh -c ': "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD is missing in container}"; : "${MYSQL_DATABASE:?MYSQL_DATABASE is missing in container}"; mysqldump --single-transaction --routines --triggers --events --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' > "$TMP_FILE"; then
  mv "$TMP_FILE" "$BACKUP_FILE"
  SIZE="$(du -h "$BACKUP_FILE" | awk '{print $1}')"
  log "Backup completed successfully."
  log "Backup file: $BACKUP_FILE"
  log "Backup size: $SIZE"
else
  log "Backup failed."
  exit 1
fi
