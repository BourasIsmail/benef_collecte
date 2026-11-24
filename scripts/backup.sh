#!/usr/bin/env bash
# Daily MySQL dump. Usage: backup.sh        -> loop, one dump per day at 02:00
#                         backup.sh once   -> one dump now (docker compose exec backup bash /backup.sh once)
set -euo pipefail
OUT=/backups
KEEP=${BACKUP_KEEP_DAYS:-14}

dump() {
  local f="$OUT/entraide-$(date +%Y%m%d-%H%M%S).sql.gz"
  MYSQL_PWD="$MYSQL_PASSWORD" mysqldump -h "$MYSQL_HOST" -u "$MYSQL_USER" --single-transaction --no-tablespaces \
    --default-character-set=utf8mb4 "$MYSQL_DATABASE" | gzip > "$f.tmp"
  mv "$f.tmp" "$f"
  find "$OUT" -name 'entraide-*.sql.gz' -mtime +"$KEEP" -delete
  echo "$(date -Iseconds) sauvegarde : $f ($(du -h "$f" | cut -f1))"
}

if [ "${1:-}" = "once" ]; then dump; exit 0; fi
while true; do
  now=$(date +%s); next=$(date -d "today 02:00" +%s)
  [ "$next" -le "$now" ] && next=$(date -d "tomorrow 02:00" +%s)
  sleep $((next - now))
  dump || echo "ECHEC de la sauvegarde" >&2
done
