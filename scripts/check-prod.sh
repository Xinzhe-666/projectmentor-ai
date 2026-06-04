#!/usr/bin/env bash
set -uo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

section() {
  printf '\n========== %s ==========\n' "$1"
}

run() {
  "$@"
  local status=$?
  if [ "$status" -ne 0 ]; then
    printf '[check-prod] Command failed with exit code %s: %s\n' "$status" "$*"
  fi
}

compose() {
  docker compose "$@"
}

section "Current Time"
date '+%Y-%m-%d %H:%M:%S %Z'

section "Git Commit"
run git rev-parse --short HEAD
run git log -1 --pretty='format:%h %cd %s' --date=iso

section "Docker Compose PS"
run compose ps

section "Container Status"
for item in \
  "backend:projectmentor-backend" \
  "frontend:projectmentor-frontend-assets" \
  "mysql:projectmentor-mysql" \
  "redis:projectmentor-redis" \
  "nginx:projectmentor-nginx"
do
  service="${item%%:*}"
  container="${item##*:}"
  printf '\n[%s] %s\n' "$service" "$container"
  if docker ps -a --format '{{.Names}}' | grep -Fxq "$container"; then
    run docker inspect -f 'status={{.State.Status}} health={{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}} restartCount={{.RestartCount}} image={{.Config.Image}}' "$container"
  else
    printf 'not found\n'
  fi
done

section "Disk Space"
run df -h

section "Memory"
run free -h

section "Docker Stats"
run docker stats --no-stream

section "Backend Logs Last 80 Lines"
run compose logs --tail=80 backend

section "Nginx Logs Last 50 Lines"
if compose config --services 2>/dev/null | grep -Fxq "nginx"; then
  run compose logs --tail=50 nginx
else
  printf 'nginx service not found in current compose config.\n'
fi

section "Common Troubleshooting Commands"
cat <<'EOF'
docker compose ps
docker compose logs -f backend
docker compose logs --tail=120 backend
docker compose logs --tail=80 nginx
docker compose logs -f mysql
docker compose logs -f redis
docker stats --no-stream
df -h
free -h
bash scripts/backup-mysql.sh
bash scripts/restore-mysql.sh backups/mysql/xxx.sql
EOF
